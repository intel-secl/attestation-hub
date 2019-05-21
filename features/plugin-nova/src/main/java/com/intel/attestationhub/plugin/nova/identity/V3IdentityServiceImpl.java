/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.identity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.intel.attestationhub.plugin.nova.Constants;
import com.intel.attestationhub.plugin.nova.identity.model.AuthRequestV3;
import com.intel.attestationhub.plugin.nova.identity.model.AuthResponseV3;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import org.apache.commons.lang3.StringUtils;

public class V3IdentityServiceImpl extends AbstractIdentityService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(V3IdentityServiceImpl.class);

    // Cache the endpoint URL to avoid parsing response multiple times
    private Map<EndpointType, String> endpointUrlMap = new HashMap<>();
    private AuthResponseV3 response;

    @Override
    public String getAuthEndpoint(String keystonePublicEndpoint) {
        return keystonePublicEndpoint + Constants.RESOURCE_PATH_V3_AUTH_TOKEN;
    }

    @Override
    public String getAuthRequestBody(String domainName, String projectName, String userName, String password)
            throws AttestationHubException {
        this.endpointUrlMap.clear();

        validateV3Params(domainName, projectName, userName, password);
        AuthRequestV3 authTokenBody = new AuthRequestV3(domainName, projectName, userName, password);
        String body;
        try {
            body = identityMapper.writeValueAsString(authTokenBody);
        } catch (JsonProcessingException e2) {
            log.error("Error while creating auth token", e2);
            throw new AttestationHubException("Error while creating auth token", e2);
        }

        return body;
    }

    @Override
    public String getEndpointUrl(EndpointType type) throws AttestationHubException {
        String endpointUrl = this.endpointUrlMap.get(type);

        if (endpointUrl == null) {
            try {
                this.response = identityMapper.readValue(this.responseStr, AuthResponseV3.class);
            } catch (ParseException | IOException e) {
                log.error("Error while parsing auth response", e);
                throw new AttestationHubException("Cannot find URL endpoint for " + type + " service.", e);
            }
            log.debug("Auth Response: {}", this.response.toString());

            endpointUrl = this.response.getEndpointUrl(type.toString());
        }

        if (StringUtils.isEmpty(endpointUrl)) {
            throw new AttestationHubException("Cannot find URL endpoint for " + type + " service.");
        }

        this.endpointUrlMap.put(type, endpointUrl);
        return endpointUrl;
    }

    @Override
    public String getAuthTokenFromResponse(HttpResponse httpResponse) throws AttestationHubException {
        return httpResponse.getFirstHeader(Constants.KEYSTONE_AUTH_TOKEN_HEADER_KEY).getValue();
    }

    public void validateV3Params(String domainName, String tenantName, String userName, String password)
           throws AttestationHubException {
        validateParams(tenantName, userName, password);
        if (StringUtils.isBlank(domainName)) {
            throw new AttestationHubException("Domain Name is required");
        }
    }
}
