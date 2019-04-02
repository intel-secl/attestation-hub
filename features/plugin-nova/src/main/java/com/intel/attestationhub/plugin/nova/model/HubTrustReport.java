/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class HubTrustReport {

    private boolean trusted;

    @JsonProperty("asset_tags")
    private Map<String, List<String>> assetTags;

    HubTrustReport() {
    }

    @JsonCreator
    public HubTrustReport(@JsonProperty("trusted") boolean trusted,
            @JsonProperty("asset_tags") Map<String, List<String>> assetTags) {
        this.trusted = trusted;
        this.assetTags = assetTags != null ? assetTags : new HashMap<>();
    }

    public boolean isTrusted() {
        return this.trusted;
    }

    public Map<String, List<String>> getAssetTags() {
        return this.assetTags;
    }

}