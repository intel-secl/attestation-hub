/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Internal model class, is NOT serialized/deserialized.
 */
public class ResourceProviderTraits extends ResourceProvider {

    private Set<String> traits = new HashSet<>();

    ResourceProviderTraits() {
    }

    public ResourceProviderTraits(String id, long generation, Set<String> traits) {
        super(id, generation);
        this.traits = new HashSet<>(traits);
    }

    public Set<String> getTraits() {
        return this.traits;
    }

    @Override
    public String toString() {
        return "ResourceProviderTraits [getId()=" + getUuid() + ", getGeneration()=" + getGeneration() + ", traits="
                + this.traits + "]";
    }

}
