/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TraitListResponse {

    private List<String> traits;

    TraitListResponse() {
    }

    @JsonCreator
    public TraitListResponse(@JsonProperty("traits") List<String> traits) {
        this.traits = traits;
    }

    public List<String> getTraits() {
        return this.traits;
    }

    @Override
    public String toString() {
        return "TraitListResponse [traits=" + this.traits + "]";
    }

}
