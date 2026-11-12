package com.fajekrina.universal.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.ZonedDateTime;

/** Read-only mapping of the view v_sdui_release_payload — the single assembly definition. */
@Getter
@Entity
@Immutable
@NoArgsConstructor
@Table(name = "v_sdui_release_payload")
public class SduiReleasePayloadView {

    @Id
    private Long releaseId;

    private String merchantId;

    private String pageKey;

    private Integer sequence;

    private Boolean isActive;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime publishedAt;

    private Integer nodeCount;

    private String unresolvedRefs;

    private String payload;
}
