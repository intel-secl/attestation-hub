/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ResourceProvider {

    private String uuid;
    private long generation;

    ResourceProvider() {
    }

    public ResourceProvider(String id, long generation) {
        this.uuid = id;
        this.generation = generation;
    }

    public String getUuid() {
        return this.uuid;
    }

    public long getGeneration() {
        return this.generation;
    }

    @Override
    public String toString() {
        return "ResourceProvider [uuid=" + this.uuid + ", generation=" + this.generation + "]";
    }

}
