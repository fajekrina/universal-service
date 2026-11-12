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

/** Registry of the 18 component types (builder palette + save-time validation). */
@Getter
@Setter
@Entity
@DynamicInsert
@Table(name = "sdui_component_type")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
public class SduiComponentType extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private String componentType;

    private String category;

    private Boolean acceptsChildren;

    @Column(columnDefinition = "CLOB")
    private String propsSchema;

    @Column(columnDefinition = "CLOB")
    private String defaultProps;

    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "CLOB")
    private List<String> allowedBehaviors;

    private String sinceSchemaVersion;

    private Boolean enabled;
}
