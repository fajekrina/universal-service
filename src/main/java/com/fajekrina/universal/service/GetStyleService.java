package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiStyle;
import com.fajekrina.universal.model.request.GetStyleRequest;
import com.fajekrina.universal.model.response.StyleResponse;
import com.fajekrina.universal.model.response.StyleResponseBuilder;
import com.fajekrina.universal.repository.SduiStyleRepository;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Style artefact by key (latest, or a fixed version). */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetStyleService {

    private final SduiStyleRepository styleRepository;

    public StyleResponse execute(GetStyleRequest request) {
        SduiStyle style = (request.version() == null
                ? styleRepository.findFirstByMerchantIdAndStyleKeyOrderByVersionDesc(request.merchantId(), request.styleKey())
                : styleRepository.findByMerchantIdAndStyleKeyAndVersion(request.merchantId(), request.styleKey(), request.version()))
                .orElseThrow(() -> new UniversalException(UniversalFault.STYLE_NOT_FOUND, "style not found: " + request.styleKey()));
        return toResponse(style);
    }

    @SuppressWarnings("unchecked")
    static StyleResponse toResponse(SduiStyle style) {
        Map<String, Object> body = CanonicalJson.readMap(style.getBody());
        return StyleResponseBuilder.builder()
                .styleKey(style.getStyleKey())
                .version(style.getVersion())
                .ref(style.getStyleKey() + "@" + style.getVersion())
                .contentHash(style.getContentHash())
                .tokens((Map<String, Object>) body.get("tokens"))
                .shared((Map<String, Object>) body.get("shared"))
                .build();
    }
}
