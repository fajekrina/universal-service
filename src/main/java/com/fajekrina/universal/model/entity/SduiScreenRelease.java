package com.fajekrina.universal.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Immutable;

/** A frozen screen release: written once, complete (signature included); DB triggers forbid UPDATE/DELETE. */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "sdui_screen_release")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
public class SduiScreenRelease extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long releaseId;

    @Column(nullable = false, updatable = false)
    private String merchantId;

    @ToString.Include
    @Column(nullable = false, updatable = false)
    private String screenKey;

    @ToString.Include
    @Column(nullable = false, updatable = false)
    private Integer sequence;

    @Column(columnDefinition = "CLOB", nullable = false, updatable = false)
    private String payload;

    @Column(nullable = false, updatable = false)
    private String contentHash;

    @Column(updatable = false)
    private String signature;

    @Column(updatable = false)
    private String signingKeyId;
}
