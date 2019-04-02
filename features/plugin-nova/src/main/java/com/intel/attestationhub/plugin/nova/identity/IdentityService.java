/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.identity;

import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public interface IdentityService {
    public static final String VERSION_V2 = "v2";
    public static final String VERSION_V3 = "v3";

    public enum EndpointType {
        PLACEMENT("placement"); // http://x.x.x.x/placement

        private String typeString;

        private EndpointType(String typeString) {
            this.typeString = typeString;
        }

        @Override
        public String toString() {
            return typeString;
        }

    }

    /**
     * Creates an auth token with the given credentials
     */
    public String createAuthToken(String keystonePublicEndpoint, String projectName, String userName,
           String password, String domainName) throws AttestationHubException;

    /**
     * Returns API endpoint URL of the type or null
     *
     * @return
     */
    public String getEndpointUrl(EndpointType type) throws AttestationHubException;

}
