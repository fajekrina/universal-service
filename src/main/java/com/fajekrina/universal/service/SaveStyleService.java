package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiStyle;
import com.fajekrina.universal.model.enums.AuditEvent;
import com.fajekrina.universal.model.request.SaveStyleRequest;
import com.fajekrina.universal.model.response.StyleResponse;
import com.fajekrina.universal.repository.SduiStyleRepository;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Validate + store a style artefact; identical content returns the existing version. */
@Service
@Transactional
@RequiredArgsConstructor
public class SaveStyleService {

    private static final Set<String> CATEGORIES = Set.of("color", "space", "radius", "font", "shadow");
    private static final Pattern TOKEN = Pattern.compile("^[a-z][a-z0-9]*(\\.[a-z0-9-]+)+$");
    private static final Pattern SHARED = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{0,63}$");
    private static final Pattern KEY = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");

    private final SduiStyleRepository styleRepository;
    private final MerchantHelperService merchantHelperService;
    private final AuditHelperService auditHelperService;

    public StyleResponse execute(SaveStyleRequest request) {
        if (!KEY.matcher(request.styleKey()).matches()) throw invalid("invalid styleKey '" + request.styleKey() + "'");
        Map<String, Object> body = normalize(request.body());
        merchantHelperService.ensureMerchant(request.merchantId());
        String hash = CanonicalJson.sha256Hex(CanonicalJson.canonical(body));
        int tokenCount = ((Map<?, ?>) body.get("tokens")).values().stream().mapToInt(m -> ((Map<?, ?>) m).size()).sum();
        SduiStyle style = styleRepository.findByMerchantIdAndStyleKeyAndContentHash(request.merchantId(), request.styleKey(), hash)
                .orElseGet(() -> {
                    SduiStyle created = styleRepository.saveAndFlush(SduiStyle.builder()
                            .merchantId(request.merchantId())
                            .styleKey(request.styleKey())
                            .version(styleRepository.findFirstByMerchantIdAndStyleKeyOrderByVersionDesc(request.merchantId(), request.styleKey())
                                    .map(s -> s.getVersion() + 1).orElse(1))
                            .body(CanonicalJson.write(body))
                            .contentHash(hash)
                            .tokenCount(tokenCount)
                            .build());
                    auditHelperService.record(request.merchantId(), null, AuditEvent.STYLE_SAVE, null,
                            Map.of("style", request.styleKey() + "@" + created.getVersion()));
                    return created;
                });
        return GetStyleService.toResponse(style);
    }

    private static UniversalException invalid(String message) {
        return new UniversalException(UniversalFault.INVALID_STYLE, message);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> normalize(Map<String, Object> incoming) {
        if (incoming == null) throw invalid("style body is required");
        Map<String, Object> tokens = new TreeMap<>();
        Set<String> seen = new HashSet<>();
        if (!(incoming.getOrDefault("tokens", Map.of()) instanceof Map<?, ?> cats)) throw invalid("tokens must be an object {category: {name: value}}");
        for (Map.Entry<?, ?> cat : cats.entrySet()) {
            String category = String.valueOf(cat.getKey());
            if (!CATEGORIES.contains(category)) throw invalid("unknown token category '" + category + "' — allowed " + new TreeSet<>(CATEGORIES));
            if (!(cat.getValue() instanceof Map<?, ?> names)) throw invalid("tokens." + category + " must be an object");
            Map<String, Object> clean = new TreeMap<>();
            for (Map.Entry<?, ?> t : names.entrySet()) {
                String name = String.valueOf(t.getKey());
                if (!TOKEN.matcher(name).matches()) throw invalid("invalid token name '" + name + "' (expected e.g. brand.primary)");
                if (name.startsWith("sdui.")) throw invalid("token namespace 'sdui.' is reserved for the library");
                if (!seen.add(name)) throw invalid("token '" + name + "' defined in more than one category");
                if (!(t.getValue() instanceof String v) || v.isBlank() || v.length() > 256) throw invalid("token '" + name + "' must be a 1-256 char string");
                clean.put(name, v);
            }
            tokens.put(category, clean);
        }
        Map<String, Object> shared = new TreeMap<>();
        if (!(incoming.getOrDefault("shared", Map.of()) instanceof Map<?, ?> sh)) throw invalid("shared must be an object {name: css}");
        for (Map.Entry<?, ?> e : sh.entrySet()) {
            String name = String.valueOf(e.getKey());
            if (!SHARED.matcher(name).matches()) throw invalid("invalid shared style name '" + name + "'");
            if (!(e.getValue() instanceof Map<?, ?> css)) throw invalid("shared." + name + " must be a CSS object");
            if ("fixed".equals(String.valueOf(((Map<String, Object>) css).get("position")))) {
                throw invalid("shared." + name + ": position:fixed is not allowed in shared styles (overlay spoofing)");
            }
            shared.put(name, new LinkedHashMap<>((Map<String, Object>) css));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tokens", tokens);
        body.put("shared", shared);
        return body;
    }
}
