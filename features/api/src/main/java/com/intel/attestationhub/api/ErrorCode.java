/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.api;

public enum ErrorCode {
    VALIDATION_FAILED("600", "Validation failed"),    
    REQUEST_PROCESSING_FAILED("601", "Request processing failed"),
    INVALID_ID("602", "Invalid ID");

    private String code;
    private String description;

    private ErrorCode(String code, String description) {
	this.code = code;
	this.description = description;
    }

    public String getErrorDescription() {
	return description;
    }

    public String getErrorCode() {
	return code;
    }
}
