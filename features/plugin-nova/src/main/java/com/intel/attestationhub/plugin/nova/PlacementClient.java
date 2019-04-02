/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.nova;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.attestationhub.plugin.nova.exception.NetworkIOException;
import com.intel.attestationhub.plugin.nova.exception.RetryPlacementCallException;
import com.intel.attestationhub.plugin.nova.identity.IdentityService;
import com.intel.attestationhub.plugin.nova.identity.IdentityService.EndpointType;
import com.intel.attestationhub.plugin.nova.identity.IdentityServiceFactory;
import com.intel.attestationhub.plugin.nova.model.ResourceProvider;
import com.intel.attestationhub.plugin.nova.model.ResourceProviderByNameResponse;
import com.intel.attestationhub.plugin.nova.model.ResourceProviderTraits;
import com.intel.attestationhub.plugin.nova.model.ResourceProviderTraitsMapping;
import com.intel.attestationhub.plugin.nova.model.TraitListResponse;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class PlacementClient {

    private static final Logger log = LoggerFactory.getLogger(PlacementClient.class);

    private static final ObjectMapper placementMapper = new ObjectMapper();

    private String authToken;
    private IdentityService identityService;

    // TODO: How to close clients on shutdown of server? Need a callback
    private static final HttpClient httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(Utils.REQUEST_CONFIG).build();

    public PlacementClient(String keystonePublicEndpoint, String projectName, String username, String password,
            String domainName, String version) throws AttestationHubException {
        Utils.validateUrl(keystonePublicEndpoint, "AUTH");
        initIdentityService(keystonePublicEndpoint, projectName, username, password, domainName, version);
    }

    /**
     * Returns all traits defined in openstack.
     */
    public Set<String> getOpenstackTraits() throws AttestationHubException {
        Set<String> osTraits = new HashSet<>();

        String url = this.identityService.getEndpointUrl(EndpointType.PLACEMENT).concat(Constants.RESOURCE_PATH_TRAITS);
        log.debug("Getting All Traits from Nova: {} ", url);

        HttpResponse response = null;
        try {
            response = getRequest(url);

            int status = response.getStatusLine().getStatusCode();

            if (HttpStatus.SC_OK != status) {
                log.error("Getting traits from controller failed with status: {}", response.getStatusLine());
                throw new AttestationHubException(
                        "Getting data from controller failed with error: " + response.getStatusLine());
            }

            TraitListResponse responseObj = placementMapper.readValue(EntityUtils.toString(response.getEntity()),
                    TraitListResponse.class);

            if (responseObj != null) {
                log.debug("Openstack Traits {}", responseObj);
                osTraits.addAll(responseObj.getTraits());
            } else {
                log.error("Error processing response when getting traits: {}", responseObj);
                throw new AttestationHubException("Error processing response from controller");
            }

        } catch (AttestationHubException rawException) {
            throw rawException;
        } catch (Exception e) {
            log.error("Getting all traits from Url: {} failed.", url, e);
            throw new AttestationHubException("Getting data from controller failed", e);
        } finally {
            HttpClientUtils.closeQuietly(response);
        }

        return osTraits;
    }

    /**
     * Creates traits on openstack from the given set of trait list. This assumes the traits are defined in the format
     * as required by openstack.
     *
     * @param traitsSet the set of traits to create
     * @throws AttestationHubException
     */
    public void createOpenstackTraits(Set<String> traitsSet) throws AttestationHubException {
        String baseUrl = this.identityService.getEndpointUrl(EndpointType.PLACEMENT)
                .concat(Constants.RESOURCE_PATH_TRAITS);

        for (String trait : traitsSet) {
            String url = baseUrl.concat("/").concat(trait);
            log.debug("Creating Trait using Url : " + url);

            HttpResponse response = null;
            try {
                response = putRequest(null, url);

                int status = response.getStatusLine().getStatusCode();

                if (!(HttpStatus.SC_OK == status || HttpStatus.SC_CREATED == status
                        || HttpStatus.SC_NO_CONTENT == status)) {
                    log.error("Creating trait failed with status: {}", response.getStatusLine());
                    throw new AttestationHubException(
                            "Sending data to controller failed with error: " + response.getStatusLine());
                }

                log.debug("Creating trait {} Status: {}", trait, status);

            } catch (AttestationHubException rethrowException) {
                throw rethrowException;
            } catch (Exception e) {
                log.error("Creating trait failed", e);
                throw new AttestationHubException("Sending data to controller failed", e);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

        }
    }

    /**
     * Queries Openstack for resource provider given the host name.
     *
     * @param hostName the host name to get the details for
     * @return the host details including its UUID and generation.
     * @throws AttestationHubException in case of any error returned by openstack
     *                                 if multiple or no resource providers are present with the requested name
     */
    public ResourceProvider getResourceProvider(String hostName) throws AttestationHubException, NetworkIOException {
        ResourceProvider provider;
        String url = this.identityService.getEndpointUrl(EndpointType.PLACEMENT)
                .concat(Constants.RESOURCE_PATH_RESOURCE_PROVIDERS_NAME_QUERY).concat(hostName);
        log.debug("Getting resource provider using Url : " + url);
        HttpResponse response = null;
        try {
            response = getRequest(url);

            int status = response.getStatusLine().getStatusCode();

            if (HttpStatus.SC_OK != status) {
                log.error("Getting resource provider by name failed with status: {}", response.getStatusLine());
                throw new AttestationHubException(
                        "Getting data from controller failed with error: " + response.getStatusLine());
            }

            ResourceProviderByNameResponse responseObj = placementMapper
                    .readValue(EntityUtils.toString(response.getEntity()), ResourceProviderByNameResponse.class);

            if (responseObj != null && responseObj.getResourceProviders() != null
                    && responseObj.getResourceProviders().size() == 1) {
                provider = responseObj.getResourceProviders().get(0);
                log.debug("ResourceProvider {}", provider);
            } else {
                log.error("Error processing response when getting resource provider by name: {}", responseObj);
                throw new AttestationHubException("Error processing response from controller");
            }

        } catch (AttestationHubException|NetworkIOException rethrowException) {
            throw rethrowException;
        } catch (Exception e) {
            log.error("Getting resource provider by name failed", e);
            throw new AttestationHubException("Getting data from controller failed", e);
        } finally {
            HttpClientUtils.closeQuietly(response);
        }

        return provider;
    }

    /**
     * Gets the traits associated with the resource provider.
     */
    public ResourceProviderTraits getResourceProviderTraits(String uuid)
            throws AttestationHubException, NetworkIOException {
        Set<String> resourceProviderTraitSet = new HashSet<>();
        long generation;

        String url = this.identityService.getEndpointUrl(EndpointType.PLACEMENT)
                .concat(Constants.RESOURCE_PATH_RESOURCE_PROVIDERS).concat(uuid)
                .concat(Constants.RESOURCE_PATH_TRAITS);
        log.debug("Getting resource providers traits from Url: {} ", url);

        HttpResponse response = null;
        try {
            response = getRequest(url);

            int status = response.getStatusLine().getStatusCode();

            if (HttpStatus.SC_OK != status) {
                log.error("Getting resource provider traits data from controller failed with status: {}",
                        response.getStatusLine());
                throw new AttestationHubException(
                        "Getting data from controller failed with error: " + response.getStatusLine());
            }

            ResourceProviderTraitsMapping responseObj = placementMapper
                    .readValue(EntityUtils.toString(response.getEntity()), ResourceProviderTraitsMapping.class);

            if (responseObj != null) {
                log.debug("Resource Provider Traits Mapping: {}", responseObj);
                generation = responseObj.getGeneration();
                resourceProviderTraitSet.addAll(responseObj.getTraits());
            } else {
                log.error("Error processing response when getting resource provider traits: {}", responseObj);
                throw new AttestationHubException("Error processing response from controller");
            }

        } catch (AttestationHubException|NetworkIOException rethrowException) {
            throw rethrowException;
        } catch (Exception e) {
            log.error("Getting resource providers traits from Url: {} failed.", url, e);
            throw new AttestationHubException("Getting data from controller failed", e);
        } finally {
            HttpClientUtils.closeQuietly(response);
        }

        ResourceProviderTraits resourceProviderTraits = new ResourceProviderTraits(uuid, generation,
                resourceProviderTraitSet);

        return resourceProviderTraits;
    }

    /**
     * Associates the resource provider with the traits provided. Assumes the traits provided are the full set of
     * traits(even non-cit managed ones)
     *
     * @param resourceProviderTraits the resource provider and the trait details
     *
     * @throws AttestationHubException incase of a unrecoverable error either with the openstack request or response
     * @throws RetryPlacementCallException incase a conflict is encountered, this allows the caller to retry the request
     *                                     by resolving the conflict or fail
     */
    public void mapResourceProviderTraits(ResourceProviderTraits resourceProviderTraits)
            throws AttestationHubException, RetryPlacementCallException, NetworkIOException {

        String url = this.identityService.getEndpointUrl(EndpointType.PLACEMENT)
                .concat(Constants.RESOURCE_PATH_RESOURCE_PROVIDERS).concat(resourceProviderTraits.getUuid())
                .concat(Constants.RESOURCE_PATH_TRAITS);
        log.debug("Mapping Traits Data to Resource Provider using url: {}", url);
        HttpResponse response = null;
        try {
            ResourceProviderTraitsMapping rpTraitsMapping = new ResourceProviderTraitsMapping(resourceProviderTraits);

            String mappingJson = placementMapper.writeValueAsString(rpTraitsMapping);

            log.debug("Traits Mapping for resource provider with id {} Json: {} ", resourceProviderTraits.getUuid(),
                    mappingJson);
            response = putRequest(mappingJson, url);
            int status = response.getStatusLine().getStatusCode();

            if(HttpStatus.SC_CONFLICT == status) {
                throw new RetryPlacementCallException();
            } else if (HttpStatus.SC_OK != status) {
                throw new AttestationHubException(
                        "Sending data to controller failed with error: " + response.getStatusLine());
            }
        } catch (RetryPlacementCallException|NetworkIOException rethrowException) {
            log.error("Mapping traits for host: {} failed with status: {}", resourceProviderTraits.getUuid(),
                    response.getStatusLine());
            throw rethrowException;
        } catch (AttestationHubException rethrowException) {
            log.error("Mapping traits for host: {} failed", resourceProviderTraits.getUuid());
            throw rethrowException;
        } catch (Exception e) {
            log.error("Mapping Traits Data failed", e);
            throw new AttestationHubException("Sending data to controller failed", e);
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    private void initIdentityService(String keystonePublicEndpoint, String projectName, String username, String password,
            String domainName, String version) throws AttestationHubException {
        this.identityService = IdentityServiceFactory.getIdentityService(IdentityService.VERSION_V3);
        if (this.identityService == null) {
            throw new AttestationHubException("No valid authentication methods found for version: " + version);
        }
        this.authToken = this.identityService.createAuthToken(keystonePublicEndpoint, projectName,
                username, password, domainName);

        log.debug("Created auth token using {} version", version);
    }


    private HttpResponse putRequest(String jsonData, String url) throws AttestationHubException, NetworkIOException {
        HttpPut putRequest = new HttpPut(url);
        log.debug("putRequest  URL : {}  jsonData: {}", putRequest.getURI(), jsonData);
        putRequest.setHeader(Constants.AUTH_TOKEN, this.authToken);
        putRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        putRequest.setHeader(Constants.OPENSTACK_API_MICROVERSION_HEADER, Constants.PLACEMENT_API_MICROVERSION_VALUE);

        if (!StringUtils.isEmpty(jsonData)) {
            try {
                HttpEntity entity = new StringEntity(jsonData);
                putRequest.setEntity(entity);
            } catch (UnsupportedEncodingException e1) {
                throw new AttestationHubException(e1);
            }
        }

        HttpResponse response;
        try {
            response = httpClient.execute(putRequest);
        } catch (IOException ioe) {
            throw new NetworkIOException("Network Error getting data from controller", ioe);
        } catch (Exception e) {
            log.error("Put request to {} failed", putRequest.getURI(), e);
            throw new AttestationHubException("Sending data to controller failed", e);
        }
        log.debug("Put Request response: {} ", response);
        return response;
    }

    private HttpResponse getRequest(String url) throws AttestationHubException, NetworkIOException {
        HttpGet getRequest = new HttpGet(url);
        log.debug("getRequest url : " + getRequest.getURI());
        getRequest.setHeader(Constants.AUTH_TOKEN, this.authToken);
        getRequest.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        getRequest.setHeader(Constants.OPENSTACK_API_MICROVERSION_HEADER, Constants.PLACEMENT_API_MICROVERSION_VALUE);

        HttpResponse response;
        try {
            response = httpClient.execute(getRequest);
        } catch (IOException ioe) {
            throw new NetworkIOException("Network Error getting data from controller", ioe);
        } catch (Exception e) {
            log.error("Get request to {} failed", getRequest.getURI(), e);
            throw new AttestationHubException("Getting data to controller failed.", e);
        }

        log.debug("Get Request response: {}", response);
        return response;
    }
}
