package com.fajekrina.universal.model.entity;

import jakarta.persistence.Column;
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

@Getter
@Setter
@Entity
@DynamicInsert
@Table(name = "sdui_merchant")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
public class SduiMerchant extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(nullable = false, updatable = false)
    private String merchantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status;

    private String defaultStyleKey;
}
