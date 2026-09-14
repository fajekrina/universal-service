package com.fajekrina.universal.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Universal SDUI PageConfig — isolated from com.sdui.demo.model.PageConfig.
 * JSON Schema 2020-12 validated on save (UniversalPageConfigService).
 *
 * layout format: { type, title?, subtitle?, components[] }
 * each component: { type: enum, id: string, props: object, style: CSSProperties object, children: [] }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UniversalPageConfig {

    private String pageId;
    private String pageKey;
    private String merchantId;
    private String status; // DRAFT / PUBLISHED
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> layout;
}
