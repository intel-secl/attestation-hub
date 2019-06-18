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

    // Contains the trait version(prefix of CUSTOM_ISECL) of all the attributes encountered
    private Set<String> customTraitsSuperSet = new HashSet<>();

    @VisibleForTesting
    NovaRsClient() {

    }

    public NovaRsClient(String keystonePublicEndpoint, String projectName, String username, String password,
            String domainName, String version) throws AttestationHubException {
        this.placementClient = new PlacementClient(keystonePublicEndpoint, projectName, username, password, domainName,
                version);
    }

    public void sendDataToEndpoint(PublishData publishData) throws AttestationHubException {
        log.debug("Sending the Traits Data to Nova");

        // Linked hash map so order is maintained...which helps with unit tests
        Map<String, Set<String>> hostCustomTraitsMap = new LinkedHashMap<>();

        for (HostDetails host : publishData.hostDetailsList) {
            Set<String> customTraits = generateTraitsFromTrustReport(host);
            this.customTraitsSuperSet.addAll(customTraits);
            hostCustomTraitsMap.put(host.hostname, customTraits);
        }

        Set<String> newTraits = Sets.difference(this.customTraitsSuperSet, this.placementClient.getOpenstackTraits());
        this.placementClient.createOpenstackTraits(newTraits);

        boolean anyErrorEncountered = false;
        List<String> networkErrorHosts = new ArrayList<>();

        for (Entry<String, Set<String>> hostEntry : hostCustomTraitsMap.entrySet()) {
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
    Set<String> generateTraitsFromTrustReport(HostDetails host) throws AttestationHubException {
        log.debug("getTraitList for {} jsonString : {}", host.hostname, host.trust_report);
        String tagPrefix = Constants.CIT_TRAIT_PREFIX + Constants.AT_PREFIX;
        String featurePrefix = Constants.CIT_TRAIT_PREFIX + Constants.HAS_PREFIX;
        StringBuffer buffer;
        Set<String> traitSet = new HashSet<>();

        try {
            HubTrustReport trustReport = trustReportWrapper.readValue(host.trust_report, HubTrustReport.class);
            if (trustReport == null) {
                throw new IOException(); // will be caught and rethrown as AttestationHubException below
            }

            if (trustReport.isTrusted()) {
                traitSet.add(Constants.CIT_TRUSTED_TRAIT);

                for (Entry<String, List<String>> tagEntry : trustReport.getAssetTags().entrySet()) {
                    String tagKey = tagEntry.getKey() // Replace any _ with 2 _. Helps differentiate A B = C -> A_B_C and A_B = C  -> A__B_C
                            .replaceAll("_",
                                    Constants.OPENSTACK_TRAITS_DELIMITER + Constants.OPENSTACK_TRAITS_DELIMITER)
                            .replaceAll(REGEX_NONSTANDARD_CHAR, Constants.OPENSTACK_TRAITS_DELIMITER);

                    if (tagEntry.getValue() != null) {
                        for (String value : tagEntry.getValue()) {
                            buffer = new StringBuffer();
                            String tagValue = value
                                    .replaceAll("_",
                                            Constants.OPENSTACK_TRAITS_DELIMITER + Constants.OPENSTACK_TRAITS_DELIMITER)
                                    .replaceAll(REGEX_NONSTANDARD_CHAR, Constants.OPENSTACK_TRAITS_DELIMITER);
                            buffer = buffer.append(tagPrefix).append(tagKey)
                                    .append(Constants.OPENSTACK_TRAITS_DELIMITER).append(tagValue);
                            traitSet.add(buffer.toString().toUpperCase());
                        }
                    }
                }

                for (Entry<String, String> featureEntry : trustReport.getHardwareFeatures().entrySet()) {
                    String featureKey = featureEntry.getKey() // Replace any _ with 2 _. Helps differentiate A B = C -> A_B_C and A_B = C  -> A__B_C
                            .replaceAll("_",
                                    Constants.OPENSTACK_TRAITS_DELIMITER + Constants.OPENSTACK_TRAITS_DELIMITER)
                            .replaceAll(REGEX_NONSTANDARD_CHAR, Constants.OPENSTACK_TRAITS_DELIMITER);

                    if (featureEntry.getValue() != null) {
                        String value = featureEntry.getValue();
                        /*
                        Add Hardware feature as trait only if it is enabled
                         */
                        if (!value.equals("false")) {
                            buffer = new StringBuffer();
                            buffer = buffer.append(featurePrefix).append(featureKey);
                            if (!value.equals("true")) {
                                String featureValue = value
                                        .replaceAll("_",
                                                Constants.OPENSTACK_TRAITS_DELIMITER + Constants.OPENSTACK_TRAITS_DELIMITER)
                                        .replaceAll(REGEX_NONSTANDARD_CHAR, Constants.OPENSTACK_TRAITS_DELIMITER);
                                buffer = buffer.append(Constants.OPENSTACK_TRAITS_DELIMITER).append(featureValue);
                            }
                            traitSet.add(buffer.toString().toUpperCase());
                        }
                    }
                }
            } else {
                log.warn("Host with name {} is not trusted, removing all existing ISECL tags", host.hostname);
            }
            log.debug("Traits for host with name {}: {} ", host.hostname, traitSet.toString());
        } catch (IOException e) {
            log.error("Error generating ISECL traits from trust report: {} .", host.trust_report, e);
            throw new AttestationHubException("Getting data from attestation server failed", e);
        }
        return traitSet;
    }

    /**
     * Associates the resource provider with the ISECL traits specified. This method automatically removes the stale ISECL
     * traits, adds/updates the valid ISECL traits while keeping the non ISECL managed traits on the host as is.
     *
     * @param resourceProviderId the resource provider id
     * @param hostName the resource provider name
     * @param latestCitTraits the latest set of ISECL traits
     * @param retriesOnConflict number of times to retry the request in case a conflict is encountered.
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
                    log.debug("Skipping nova call since the host {} is already associated with the ISECL traits",
                            hostName);
                }
                break;
            } catch (RetryPlacementCallException retryException) {
                log.warn("Updating traits for host {} failed due to conflict. Retrying the call. "
                        + "Number of retries remaining: {}", hostName, tries - 1);
                try {
                    Thread.sleep(Constants.CONFLICT_RETRY_DELAY_IN_MILLIS);
                } catch (InterruptedException e) {
                    log.error("Interrupted while waiting to retry mapping ISECL traits to host {}", hostName);
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
     * Note: the updated traits will contain the full set of traits(including non-isecl traits)
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
