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

    @JsonProperty("hardware_features")
    private Map<String, String> hardwareFeatures;

    HubTrustReport() {
    }

    @JsonCreator
    public HubTrustReport(@JsonProperty("trusted") boolean trusted,
                          @JsonProperty("asset_tags") Map<String, List<String>> assetTags,
                          @JsonProperty("hardware_features") Map<String, String> hardwareFeatures) {
        this.trusted = trusted;
        this.assetTags = assetTags != null ? assetTags : new HashMap<>();
        this.hardwareFeatures = hardwareFeatures != null ? hardwareFeatures : new HashMap<>();
    }

    public boolean isTrusted() {
        return this.trusted;
    }

    public Map<String, List<String>> getAssetTags() {
        return this.assetTags;
    }

    public Map<String, String> getHardwareFeatures() {
        return this.hardwareFeatures;
    }

}