/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.identity;

import java.util.Date;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.attestationhub.plugin.nova.Utils;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

abstract class AbstractIdentityService implements IdentityService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractIdentityService.class);

    protected static final ObjectMapper identityMapper = new ObjectMapper();

    protected String responseStr;
    // Share HTTP client
    private static final HttpClient httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(Utils.REQUEST_CONFIG).build();

    public abstract String getAuthEndpoint(String keystonePublicEndpoint);

    public abstract String getAuthRequestBody(String domainName, String projectName, String userName, String password)
            throws AttestationHubException;

    public abstract String getAuthTokenFromResponse(HttpResponse httpResponse) throws AttestationHubException;

    @Override
    public String createAuthToken(String keystoneEndpoint, String projectName, String userName, String password,
            String domainName) throws AttestationHubException {

        long start = new Date().getTime();
        HttpResponse httpResponse = null;
        try {
            String authEndpoint = getAuthEndpoint(keystoneEndpoint);

            Utils.validateUrl(keystoneEndpoint, "AUTH");

            String body = getAuthRequestBody(domainName, projectName, userName, password);

            HttpEntity entity = new StringEntity(body);

            HttpPost postRequest = new HttpPost(authEndpoint);
            postRequest.setEntity(entity);
            postRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            postRequest.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

            httpResponse = httpClient.execute(postRequest);
            if (HttpStatus.SC_OK != httpResponse.getStatusLine().getStatusCode()
                    && HttpStatus.SC_CREATED != httpResponse.getStatusLine().getStatusCode()) {

                log.error("Unable to fetch token by {} , statusline: {}", authEndpoint,
                        httpResponse.getStatusLine());
                throw new AttestationHubException("Unable to authenticate");

            }

            this.responseStr = EntityUtils.toString(httpResponse.getEntity());
            String authToken = getAuthTokenFromResponse(httpResponse);

            long end = new Date().getTime();
            printTimeDiff("createAuthToken", start, end);

            return authToken;
        } catch (Exception e) {
            log.error("Error while creating auth token", e);
            throw new AttestationHubException("Error while creating auth token", e);
        } finally {
            HttpClientUtils.closeQuietly(httpResponse);
        }
    }

    private void printTimeDiff(String method, long start, long end) {
        log.debug(method + " took " + (end - start) + " ms");
    }

    protected void validateParams(String tenantName, String userName, String password) throws AttestationHubException {
        if (StringUtils.isBlank(tenantName)) {
            throw new AttestationHubException("Project Name cannot be blank");
        }
        if (StringUtils.isBlank(userName)) {
            throw new AttestationHubException("User Name cannot be blank");
        }
        if (StringUtils.isBlank(password)) {
            throw new AttestationHubException("Password cannot be blank");
        }

    }
}
