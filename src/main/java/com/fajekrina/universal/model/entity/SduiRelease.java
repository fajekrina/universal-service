package com.fajekrina.universal.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.ZonedDateTime;

/**
 * Atomic snapshot of a page. Everything is immutable (DB trigger) except the
 * fill-once columns: payload/contentHash at publish, signature/signingKeyId after signing.
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "sdui_release")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class SduiRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long releaseId;

    @Column(nullable = false, updatable = false)
    private String pageId;

    @Column(nullable = false, updatable = false)
    private String merchantId;

    @ToString.Include
    @Column(nullable = false, updatable = false)
    private Integer sequence;

    @Column(nullable = false, updatable = false)
    private Long treeId;

    @Column(updatable = false)
    private Long styleId;

    @Column(updatable = false)
    private Long scriptId;

    @Column(nullable = false, updatable = false)
    private Integer policyVersion;

    @Column(columnDefinition = "CLOB")
    private String payload;

    private String contentHash;

    private String signature;

    private String signingKeyId;

    @Column(updatable = false)
    private Long rollbackOf;

    @Column(nullable = false, updatable = false)
    private String publishedBy;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false, updatable = false)
    private ZonedDateTime publishedAt;

    @Column(updatable = false)
    private String note;
}
