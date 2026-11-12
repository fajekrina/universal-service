package com.fajekrina.universal.model.entity;

import com.fajekrina.universal.model.enums.AuditEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Append-only audit trail; createdBy = actor. */
@Getter
@Setter
@Entity
@Immutable
@DynamicInsert
@Table(name = "sdui_audit_log")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
public class SduiAuditLog extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long auditId;

    @Column(nullable = false)
    private String merchantId;

    private String pageId;

    @ToString.Include
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditEvent event;

    private Long releaseId;

    private String requestId;

    @Column(columnDefinition = "CLOB", nullable = false)
    private String detail;
}
