package com.fajekrina.universal.model.entity;

import com.fajekrina.universal.utils.converter.JsonStringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.util.List;

/** Closed action vocabulary. */
@Getter
@Setter
@Entity
@DynamicInsert
@Table(name = "sdui_behavior")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
public class SduiBehavior extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private String behavior;

    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "CLOB")
    private List<String> requiredFields;

    private String policyCheck;

    private String description;

    private Boolean enabled;
}
