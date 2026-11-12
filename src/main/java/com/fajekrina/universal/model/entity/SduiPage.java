package com.fajekrina.universal.model.entity;

import com.fajekrina.universal.model.enums.PageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;

import java.time.ZonedDateTime;

/** The only mutable page row: pointers to the draft artefacts and the active release. */
@Getter
@Setter
@Entity
@DynamicInsert
@Table(name = "sdui_page")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
public class SduiPage extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(nullable = false, updatable = false)
    private String pageId;

    @Column(nullable = false, updatable = false)
    private String merchantId;

    @ToString.Include
    @Column(nullable = false, updatable = false)
    private String pageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PageStatus status;

    private Long draftTreeId;

    private Long draftScriptId;

    private Long draftStyleId;

    private Long activeReleaseId;

    @Column(name = "archived_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime archivedTime;
}
