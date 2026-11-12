package com.fajekrina.universal.model.entity;

import jakarta.persistence.Entity;
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

/** Versioned validation bounds (insert-only). */
@Getter
@Setter
@Entity
@Immutable
@DynamicInsert
@Table(name = "sdui_bounds")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
public class SduiBounds extends CreatedEntity {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer boundsVersion;

    private Integer maxDepth;
    private Integer maxNodes;
    private Integer maxStringLength;
    private Integer maxProps;
    private Integer maxChildren;
    private Integer maxArrayItems;
    private Integer maxIdentifierLength;
    private Integer maxChainDepth;
}
