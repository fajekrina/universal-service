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

/** Tree artefact (blob): {schemaVersion, layout, nodes[]} — insert-only, owned by a page. */
@Getter
@Setter
@Entity
@Immutable
@DynamicInsert
@Table(name = "sdui_tree")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
public class SduiTree extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long treeId;

    @Column(nullable = false)
    private String pageId;

    @Column(nullable = false)
    private String merchantId;

    @ToString.Include
    @Column(nullable = false)
    private Integer version;

    @Column(columnDefinition = "CLOB", nullable = false)
    private String body;

    @Column(nullable = false)
    private String contentHash;

    @Column(nullable = false)
    private String schemaVersion;

    @Column(nullable = false)
    private Integer nodeCount;

    @Column(nullable = false)
    private Integer maxDepth;

    private String note;
}
