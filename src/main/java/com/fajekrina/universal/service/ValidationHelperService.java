package com.fajekrina.universal.service;

import com.fajekrina.universal.model.dto.ComponentRegistry;
import com.fajekrina.universal.model.dto.ValidationBounds;
import com.fajekrina.universal.model.entity.SduiBehavior;
import com.fajekrina.universal.model.entity.SduiComponentType;
import com.fajekrina.universal.repository.SduiBehaviorRepository;
import com.fajekrina.universal.repository.SduiBoundsRepository;
import com.fajekrina.universal.repository.SduiComponentTypeRepository;
import com.fajekrina.universal.utils.UniversalPayloadValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Builds the save-time validator from registry data (component types, behaviors, bounds). */
@Service
@RequiredArgsConstructor
public class ValidationHelperService {

    private final SduiComponentTypeRepository componentTypeRepository;
    private final SduiBehaviorRepository behaviorRepository;
    private final SduiBoundsRepository boundsRepository;

    public UniversalPayloadValidator validator() {
        Map<String, Set<String>> byType = new LinkedHashMap<>();
        Set<String> containers = new HashSet<>();
        for (SduiComponentType t : componentTypeRepository.findAllByOrderByComponentType()) {
            byType.put(t.getComponentType(), new HashSet<>(t.getAllowedBehaviors()));
            if (Boolean.TRUE.equals(t.getAcceptsChildren())) containers.add(t.getComponentType());
        }
        Set<String> behaviors = behaviorRepository.findAllByEnabledTrue().stream()
                .map(SduiBehavior::getBehavior).collect(Collectors.toCollection(TreeSet::new));
        ValidationBounds bounds = boundsRepository.findFirstByOrderByBoundsVersionDesc()
                .map(b -> new ValidationBounds(b.getMaxDepth(), b.getMaxNodes(), b.getMaxStringLength(), b.getMaxProps(),
                        b.getMaxChildren(), b.getMaxArrayItems(), b.getMaxIdentifierLength(), b.getMaxChainDepth()))
                .orElse(ValidationBounds.DEFAULT);
        return new UniversalPayloadValidator(bounds, new ComponentRegistry(byType, containers, behaviors));
    }

    public void validateOrThrow(Map<String, Object> layout, Map<String, Object> policy) {
        validator().validateOrThrow(layout, policy);
    }
}
