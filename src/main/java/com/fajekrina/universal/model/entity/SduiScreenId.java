package com.fajekrina.universal.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Composite key of {@link SduiScreen}: (merchantId, screenKey). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SduiScreenId implements Serializable {
    private String merchantId;
    private String screenKey;
}
