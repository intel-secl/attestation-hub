/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.intel.attestationhub.api.HostDetails;
import com.intel.attestationhub.api.PublishData;
import com.intel.attestationhub.plugin.nova.exception.NetworkIOException;
import com.intel.attestationhub.plugin.nova.exception.RetryPlacementCallException;
import com.intel.attestationhub.plugin.nova.model.HubTrustReport;
import com.intel.attestationhub.plugin.nova.model.ResourceProvider;
import com.intel.attestationhub.plugin.nova.model.ResourceProviderTraits;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class NovaRsClient {

    private static final String REGEX_NONSTANDARD_CHAR = "[^a-zA-Z0-9]";
    private static final ObjectMapper trustReportWrapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    private static final Logger log = LoggerFactory.getLogger(NovaRsClient.class);

    private PlacementClient placementClient;

    // Contains the trait version(prefix of CUSTOM_CIT) of all the asset tags encountered
    private Set<String> assetTagTraitsSuperSet = new HashSet<>();

    @VisibleForTesting
    NovaRsClient() {

    }

    public NovaRsClient(String keystonePublicEndpoint, String projectName, String username, String password,
            String domainName, String version) throws AttestationHubException {
        this.placementClient = new PlacementClient(keystonePublicEndpoint, projectName, username, password, domainName,
                version);
    }

    public void sendDataToEndpoint(PublishData publishData) throws AttestationHubException {
        log.debug("Sending to Traits Data to Nova");

        // Linked hash map so order is maintained...which helps with unit tests
        Map<String, Set<String>> hostCitTraitsMap = new LinkedHashMap<>();

        for (HostDetails host : publishData.hostDetailsList) {
            Set<String> assetTagTraits = generateTraitsFromAssetTags(host);
            this.assetTagTraitsSuperSet.addAll(assetTagTraits);
            hostCitTraitsMap.put(host.hostname, assetTagTraits);
        }

        Set<String> newTraits = Sets.difference(this.assetTagTraitsSuperSet, this.placementClient.getOpenstackTraits());
        this.placementClient.createOpenstackTraits(newTraits);

        boolean anyErrorEncountered = false;
        List<String> networkErrorHosts = new ArrayList<>();

        for (Entry<String, Set<String>> hostEntry : hostCitTraitsMap.entrySet()) {
            String hostName = hostEntry.getKey();
            Set<String> latestCitTraits = hostEntry.getValue();
            try {
                log.debug("HostName: {} :: Traits: {} ", hostName, latestCitTraits);
                ResourceProvider hostRp = this.placementClient.getResourceProvider(hostName);
                mapHostTraits(hostRp.getUuid(), hostName, latestCitTraits, Constants.MAX_RETRIES_DUE_TO_CONFLICTS);
                log.info("Updating traits for host {} succeeded", hostName);
                // Reset list if we have a successful call
                networkErrorHosts = new ArrayList<>();
            } catch (NetworkIOException ioe) {
                anyErrorEncountered = true;
                networkErrorHosts.add(hostName);
                if (networkErrorHosts.size() > Constants.NO_OF_FAILED_CALLS_BEFORE_FAILING_BATCH) {
                    log.error(
                            "Too many network errors encountered for the current batch of hosts: {}. Failing the entire batch.",
                            String.join(", ", networkErrorHosts));
                    throw new AttestationHubException(
                            "Too many network errors encountered for the current batch of hosts. Failing the entire batch.");
                }
            } catch (AttestationHubException ae) {
                anyErrorEncountered = true;
                log.warn("Failed to update traits for host {} due to unknown error. Will continue to process other "
                        + "hosts.", hostName, ae);
            }
        }

        // Still throw an exception because few host mapping failed.
        if(anyErrorEncountered) {
            throw new AttestationHubException("Unable to map a few hosts with their asset tags, check log for more "
                    + "details");
        }

        log.debug("Sending Traits Data to Nova completed");
    }

    @VisibleForTesting
    Set<String> generateTraitsFromAssetTags(HostDetails host) throws AttestationHubException {
        log.debug("getTraitList for {} jsonString : {}", host.hostname, host.trust_report);
        String prefix = Constants.CIT_TRAIT_PREFIX + Constants.AT_PREFIX;
        StringBuffer assetBuffer;
        Set<String> traitSet = new HashSet<>();

        try {
            HubTrustReport trustReport = trustReportWrapper.readValue(host.trust_report, HubTrustReport.class);
            if (trustReport == null) {
                throw new IOException(); // will be caught and rethrown as AttestationHubException below
            }

            if (trustReport.isTrusted()) {
                traitSet.add(Constants.CIT_TRUSTED_TRAIT);

                for (Entry<String, List<String>> tagEntry : trustReport.getAssetTags().entrySet()) {
                    String assetKey = tagEntry.getKey() // Replace any _ with 2 _. Helps differentiate A B = C -> A_B_C and A_B = C  -> A__B_C
                            .replaceAll("_",
                                    Constants.OPENSTACK_TRAITS_DELIMITER + Constants.OPENSTACK_TRAITS_DELIMITER)
                            .replaceAll(REGEX_NONSTANDARD_CHAR, Constants.OPENSTACK_TRAITS_DELIMITER);

                    if (tagEntry.getValue() != null) {
                        for (String asset : tagEntry.getValue()) {
                            assetBuffer = new StringBuffer();
                            String assetString = asset
                                    .replaceAll("_",
                                            Constants.OPENSTACK_TRAITS_DELIMITER + Constants.OPENSTACK_TRAITS_DELIMITER)
                                    .replaceAll(REGEX_NONSTANDARD_CHAR, Constants.OPENSTACK_TRAITS_DELIMITER);
                            assetBuffer = assetBuffer.append(prefix).append(assetKey)
                                    .append(Constants.OPENSTACK_TRAITS_DELIMITER).append(assetString);
                            traitSet.add(assetBuffer.toString().toUpperCase());
                        }
                    }
                }
            } else {
                log.warn("Host with name {} is not trusted, removing all existing CIT tags", host.hostname);
            }
            log.debug("Traits for host with name {}: {} ", host.hostname, traitSet.toString());
        } catch (IOException e) {
            log.error("Error generating CIT traits from trust report: {} .", host.trust_report, e);
            throw new AttestationHubException("Getting data from attestation server failed", e);
        }
        return traitSet;
    }

    /**
     * Associates the resource provider with the CIT traits specified. This method automatically removes the stale CIT
     * traits, adds/updates the valid CIT traits while keeping the non CIT managed traits on the host as is.
     *
     * @param resourceProviderId the resource provider id
     * @param hostName the resource provider name
     * @param latestCitTraits the latest set of CIT traits
     * @param tries number of times to retry the request in case a conflict is encountered.
     *
     * @throws AttestationHubException in case of any errors(other than 409 conflict, for which we retry) happen
     */
    private void mapHostTraits(String resourceProviderId, String hostName, Set<String> latestCitTraits,
            int retriesOnConflict) throws AttestationHubException, NetworkIOException {
        int tries = retriesOnConflict;
        while (tries > 0) {
            try {
                ResourceProviderTraits hostTraits = this.placementClient.getResourceProviderTraits(resourceProviderId);
                Set<String> updatedTraits = getUpdatedTraits(hostTraits.getTraits(), latestCitTraits);
                if (updatedTraits != null) {
                    this.placementClient.mapResourceProviderTraits(new ResourceProviderTraits(hostTraits.getUuid(),
                            hostTraits.getGeneration(), updatedTraits));
                    log.debug("Updating traits for host {} succeeded with {} retries", hostName, retriesOnConflict - tries);
                } else {
                    log.debug("Skipping nova call since the host {} is already associated with the CIT traits",
                            hostName);
                }
                break;
            } catch (RetryPlacementCallException retryException) {
                log.warn("Updating traits for host {} failed due to conflict. Retrying the call. "
                        + "Number of retries remaining: {}", hostName, tries - 1);
                try {
                    Thread.sleep(Constants.CONFLICT_RETRY_DELAY_IN_MILLIS);
                } catch (InterruptedException e) {
                    log.error("Interrupted while waiting to retry mapping CIT traits to host {}", hostName);
                    throw new AttestationHubException("Sending data to controller failed", e);
                }
            }
            tries--;
            if (tries == 0) {
                throw new AttestationHubException("Sending data to controller failed");
            }
        }
    }

    /**
     * Gets updated traits for the host only if they need to be updated. If no updates are needed, returns null.
     *
     * Note: the updated traits will contain the full set of traits(including non-cit traits)
     */
    private Set<String> getUpdatedTraits(Set<String> resourceProviderTraits, Set<String> citTraits) {
        Set<String> updatedTraits = null;
        Set<String> commonTraits = Sets.intersection(resourceProviderTraits, citTraits);
        Set<String> newTraitsToAdd = Sets.difference(citTraits, commonTraits);
        Set<String> staleTraitsOnHost = Sets.difference(resourceProviderTraits, commonTraits).stream()
                .filter(trait -> trait.startsWith(Constants.CIT_TRAIT_PREFIX)).collect(Collectors.toSet());

        if (!newTraitsToAdd.isEmpty() || !staleTraitsOnHost.isEmpty()) {
            updatedTraits = new HashSet<>(resourceProviderTraits);
            updatedTraits.removeAll(staleTraitsOnHost);
            updatedTraits.addAll(newTraitsToAdd);
        }

        return updatedTraits;

    }

}