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
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Immutable;

/** Style artefact (blob): {tokens{}, shared{}} — insert-only, owned by a merchant. */
@Getter
@Setter
@Entity
@Immutable
@DynamicInsert
@Table(name = "sdui_style")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
public class SduiStyle extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long styleId;

    @Column(nullable = false)
    private String merchantId;

    @ToString.Include
    @Column(nullable = false)
    private String styleKey;

    @ToString.Include
    @Column(nullable = false)
    private Integer version;

    @Column(columnDefinition = "CLOB", nullable = false)
    private String body;

    @Column(nullable = false)
    private String contentHash;

    @Column(nullable = false)
    private Integer tokenCount;

    private String note;
}
