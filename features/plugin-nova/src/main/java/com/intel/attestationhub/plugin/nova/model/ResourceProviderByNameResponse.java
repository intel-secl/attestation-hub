/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ResourceProviderByNameResponse {

    private List<ResourceProvider> resourceProviders;

    ResourceProviderByNameResponse() {
    }

    @JsonCreator
    public ResourceProviderByNameResponse(@JsonProperty("resource_providers") List<ResourceProvider> resourceProviders) {
        this.resourceProviders = resourceProviders;
    }

    public List<ResourceProvider> getResourceProviders() {
        return this.resourceProviders;
    }

    @Override
    public String toString() {
        return "ResourceProviderByNameResponse [resourceProviders=" + this.resourceProviders + "]";
    }
}
