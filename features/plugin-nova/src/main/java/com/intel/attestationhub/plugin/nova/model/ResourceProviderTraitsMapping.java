/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ResourceProviderTraitsMapping {

    @JsonProperty("resource_provider_generation")
    private long generation;
    private List<String> traits;

    ResourceProviderTraitsMapping() {
    }

    @JsonCreator
    public ResourceProviderTraitsMapping(@JsonProperty("resource_provider_generation") long generation,
            @JsonProperty("traits") List<String> traits) {
        this.traits = traits;
        this.generation = generation;
    }

    public ResourceProviderTraitsMapping(ResourceProviderTraits resourceProviderTraits) {
        this(resourceProviderTraits.getGeneration(), new ArrayList<>(resourceProviderTraits.getTraits()));
    }

    public long getGeneration() {
        return this.generation;
    }

    public List<String> getTraits() {
        return this.traits;
    }

    @Override
    public String toString() {
        return "ResourceProviderTraitsMapping [generation=" + this.generation + ", traits=" + this.traits + "]";
    }

}
