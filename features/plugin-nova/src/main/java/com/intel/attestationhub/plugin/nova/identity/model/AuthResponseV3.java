/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.identity.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Authentication response
 */
public class AuthResponseV3 {

    @JsonIgnoreProperties(ignoreUnknown=true)
    private static class Token {
        public List<Catalog> catalog;

        @Override
        public String toString() {
            return "Token [catalog=" + this.catalog + "]";
        }

    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    private static class Catalog {
        public List<Endpoint> endpoints;
        public String type;

        @Override
        public String toString() {
            return "Catalog [endpoints=" + this.endpoints + ", type=" + this.type + "]";
        }

    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    private static class Endpoint {
        public String url;

        @JsonProperty("interface")
        public String interfaceType;

        @Override
        public String toString() {
            return "Endpoint [url=" + this.url + ", interfaceType=" + this.interfaceType + "]";
        }
    }

    public Token token;

    public String getEndpointUrl(String type) {

        if (this.token != null && this.token.catalog != null) {
            for (Catalog catalog : this.token.catalog) {
                if (type.equalsIgnoreCase(catalog.type) && catalog.endpoints != null) {
                    for (Endpoint endPoint : catalog.endpoints) {
                        // TODO: Should we return only admin interface?
                        return endPoint.url;
                    }
                }

            }
        }
        return null;
    }


    @Override
    public String toString() {
        return "AuthResponseV3 [token=" + this.token + "]";
    }
}
