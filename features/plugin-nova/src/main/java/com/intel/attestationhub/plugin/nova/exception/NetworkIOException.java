/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.exception;

/**
 * Indicates a network io exception during placement call(in case of timeout for example)
 */
public class NetworkIOException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public NetworkIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetworkIOException(Throwable cause) {
        super(cause);
    }

}
