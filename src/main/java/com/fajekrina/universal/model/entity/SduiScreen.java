package com.fajekrina.universal.model.entity;

import com.fajekrina.universal.model.enums.PageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * A spec screen ({schemaVersion, screen}): the editable draft as one canonical
 * document, plus a pointer to the active release. Optimistic lock: {@code version}.
 */
@Getter
@Setter
@Entity
@DynamicInsert
@IdClass(SduiScreenId.class)
@Table(name = "sdui_screen")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
public class SduiScreen extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(nullable = false, updatable = false)
    private String merchantId;

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(nullable = false, updatable = false)
    private String screenKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PageStatus status;

    @Column(columnDefinition = "CLOB", nullable = false)
    private String draft;

    @Column(nullable = false)
    private String draftHash;

    private Long activeReleaseId;
}
