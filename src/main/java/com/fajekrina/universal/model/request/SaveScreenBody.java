package com.fajekrina.universal.model.request;

/** HTTP body of {@code POST merchant/screens/{mid}/{key}}. */
public record SaveScreenBody(Object payload, Integer lockVersion) {}
