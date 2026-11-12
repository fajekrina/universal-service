package com.fajekrina.universal.model.entity;

import com.fajekrina.universal.utils.converter.JsonStringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
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

import java.util.List;

/** Versioned allow-lists per merchant (insert-only). */
@Getter
@Setter
@Entity
@Immutable
@DynamicInsert
@IdClass(SduiPolicyId.class)
@Table(name = "sdui_policy")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
public class SduiPolicy extends CreatedEntity {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private String merchantId;

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer policyVersion;

    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "CLOB", nullable = false)
    private List<String> allowedDomains;

    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "CLOB", nullable = false)
    private List<String> allowedEvents;

    @Column(nullable = false)
    private Integer boundsVersion;
}
