package com.secureops.model;

public enum Severity {
    INFO, LOW, MEDIUM, HIGH, CRITICAL;

    public int rank() { return ordinal(); }
}
