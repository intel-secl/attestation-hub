/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.api;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * "host_trust_response": { "hostname": "192.168.0.1", "trust": { "bios": true,
 * "vmm": true, "location": false } }
 * 
 * @author GS-0681
 *
 */
public class HostTrustResponse {
    @JsonProperty("hostname")
    private String hostName;
    @JsonProperty("trust")
    private Map<String, String> flavorTrustStatus;
    @JsonProperty("valid_to")
    private String validTo;
    @JsonProperty("trusted")
    private boolean trusted;
    @JsonProperty("asset_tags")
    private Map<String, List<String>> assetTags;
    @JsonProperty("hardware_features")
    private Map<String, String> hardwareFeatures;

    public String getHostName() {
	return hostName;
    }

    public void setHostName(String hostName) {
	this.hostName = hostName;
    }

    public Map<String, String> getFlavorTrustStatus() {
        return flavorTrustStatus;
    }

    public void setFlavorTrustStatus(Map<String, String> flavorTrustStatus) {
        this.flavorTrustStatus = flavorTrustStatus;
    }

    public String getValidTo() {
	return validTo;
    }

    public void setValidTo(String validTo) {
	this.validTo = validTo;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    public Map<String, List<String>> getAssetTags() {
        return assetTags;
    }

    public void setAssetTags(Map<String, List<String>> assetTags) {
        this.assetTags = assetTags;
    }

    public Map<String, String> getHardwareFeatures() {
        return hardwareFeatures;
    }

    public void setHardwareFeatures(Map<String, String> hardwareFeatures) {
        this.hardwareFeatures = hardwareFeatures;
    }

}
