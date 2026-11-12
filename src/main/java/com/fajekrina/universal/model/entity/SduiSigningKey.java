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

import java.time.ZonedDateTime;

/** Public verification keys (the private key stays in configuration). */
@Getter
@Setter
@Entity
@DynamicInsert
@Table(name = "sdui_signing_key")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
public class SduiSigningKey extends CreatedEntity {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private String keyId;

    private String merchantId;

    private String algorithm;

    @Column(nullable = false)
    private String publicKey;

    private String status;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime retiredAt;
}
