package com.fajekrina.universal.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SduiPolicyId implements Serializable {
    private String merchantId;
    private Integer policyVersion;
}
