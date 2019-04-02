/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intel.mtwilson.model.Hostname;
import java.util.Map;

/**
 *
 * @author purvades
 */
public class FlavorStatusResponse
{
    @JsonProperty("hostname") public Hostname hostname;
    @JsonProperty("trust") public Map<String, String> trust;

    public FlavorStatusResponse() {
        
    }
    
    public FlavorStatusResponse(Hostname hostname, Map<String, String> trust) {
        this.hostname = hostname;
        this.trust = trust;
    }    
}

