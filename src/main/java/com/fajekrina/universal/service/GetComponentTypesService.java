package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiBehavior;
import com.fajekrina.universal.model.entity.SduiComponentType;
import com.fajekrina.universal.model.response.ComponentTypesResponse;
import com.fajekrina.universal.model.response.ComponentTypesResponseBuilder;
import com.fajekrina.universal.model.response.ComponentTypesResponseComponentTypeBuilder;
import com.fajekrina.universal.repository.SduiBehaviorRepository;
import com.fajekrina.universal.repository.SduiComponentTypeRepository;
import com.fajekrina.universal.utils.CanonicalJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Builder palette: enabled component types + behavior vocabulary. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetComponentTypesService {

    private final SduiComponentTypeRepository componentTypeRepository;
    private final SduiBehaviorRepository behaviorRepository;

    public ComponentTypesResponse execute() {
        Map<String, ComponentTypesResponse.ComponentType> components = new LinkedHashMap<>();
        for (SduiComponentType t : componentTypeRepository.findAllByOrderByComponentType()) {
            if (!Boolean.TRUE.equals(t.getEnabled())) continue;
            components.put(t.getComponentType(), ComponentTypesResponseComponentTypeBuilder.builder()
                    .category(t.getCategory())
                    .acceptsChildren(Boolean.TRUE.equals(t.getAcceptsChildren()))
                    .allowedBehaviors(t.getAllowedBehaviors())
                    .props(CanonicalJson.readMap(t.getDefaultProps()))
                    .build());
        }
        return ComponentTypesResponseBuilder.builder()
                .components(components)
                .behaviors(behaviorRepository.findAllByEnabledTrue().stream().map(SduiBehavior::getBehavior)
                        .collect(Collectors.toCollection(TreeSet::new)))
                .build();
    }
}
