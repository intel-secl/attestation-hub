/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import com.google.common.collect.Sets;
import com.intel.attestationhub.api.HostDetails;
import com.intel.attestationhub.api.PublishData;
import com.intel.attestationhub.plugin.nova.exception.NetworkIOException;
import com.intel.attestationhub.plugin.nova.exception.RetryPlacementCallException;
import com.intel.attestationhub.plugin.nova.model.ResourceProvider;
import com.intel.attestationhub.plugin.nova.model.ResourceProviderTraits;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class NovaRsClientTest {

    private static final String ADDITIONAL_HOSTNAME_PREFIX = "hostname-add";

    private static final String STANDARD_TRAIT_YY = "CPU_YY";

    private static final String STANDARD_TRAIT_XX = "HW_XX";

    private static final String HOST3_ID = "host3_id";

    private static final String HOST2_ID = "host2_id";

    private static final String HOST1_ID = "host1_id";

    private static final String HOSTNAME3 = "host3";

    private static final String HOSTNAME2 = "hostname2";

    private static final String HOSTNAME1 = "hostname1";

    private static final Set<String> HOST1_CIT_TAGS = Sets.newHashSet("ISECL_TRUSTED", "ISECL_AT_STATE_AZ",
            "ISECL_AT_CITY_AUSTIN", "ISECL_AT_COUNTRY_US", "ISECL_AT_STATE_TX",
            "ISECL_AT_CITY_HILLSBORO", "ISECL_AT_STATE_OR");

    private static final Set<String> HOST1_CIT_UPDATED_TAGS = Sets.newHashSet("ISECL_TRUSTED",
            "ISECL_AT_STATE_AZ", "ISECL_AT_CITY_SANTA_CLARA", "ISECL_AT_COUNTRY_US",
            "ISECL_AT_STATE_TX", "ISECL_AT_STATE_OR");

    private static final Set<String> HOST2_CIT_TAGS = Sets.newHashSet("ISECL_AT_STATE_TN",
            "ISECL_AT_CITY_HYDERABAD", "ISECL_AT_STATE_KA", "ISECL_AT_COUNTRY_IN", "ISECL_TRUSTED",
            "ISECL_AT_CITY_BANGALORE", "ISECL_AT_STATE_AP");

    private static final Set<String> NEW_TRAITS_ON_OS = Sets.union(HOST1_CIT_TAGS, HOST2_CIT_TAGS);

    private PublishData data;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    private static final String TRUST_FOLDER = "trust" + File.separator;

    @Mock
    private PlacementClient placementClient;

    @InjectMocks
    private NovaRsClient novaClient = new NovaRsClient(); // need construct manually since we want to use property injection

    @Captor
    private ArgumentCaptor<Set<String>> newTraitsCaptor;

    @Captor
    private ArgumentCaptor<ResourceProviderTraits> rpTraitsCaptor;

    @Before
    public void setUp() throws Exception {
        // Create publishdata
        this.data = new PublishData();
        this.data.hostDetailsList = new ArrayList<>();
        this.data.hostDetailsList
                .add(generateHostDetails(TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags.json"), HOSTNAME1));
        this.data.hostDetailsList.add(
                generateHostDetails(TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags2.json"), HOSTNAME2));

        when(this.placementClient.getOpenstackTraits()).thenReturn(Sets.newHashSet(STANDARD_TRAIT_XX, STANDARD_TRAIT_YY));

        doReturn(new ResourceProvider(HOST1_ID, 0)).when(this.placementClient).getResourceProvider(HOSTNAME1);
        doReturn(new ResourceProvider(HOST2_ID, 0)).when(this.placementClient).getResourceProvider(HOSTNAME2);

        // configure host 1 and host 2 to have existing traits.
        doReturn(new ResourceProviderTraits(HOST1_ID, 0, Sets.newHashSet(STANDARD_TRAIT_XX))).when(this.placementClient)
                .getResourceProviderTraits(HOST1_ID);
        doReturn(new ResourceProviderTraits(HOST2_ID, 0, Sets.newHashSet(STANDARD_TRAIT_YY))).when(this.placementClient)
                .getResourceProviderTraits(HOST2_ID);

    }

    @Test
    public void testSendDataToEndpoint_WithTrustedHostAndTags_ExpectSuccess()
            throws AttestationHubException, RetryPlacementCallException, NetworkIOException {

        this.novaClient.sendDataToEndpoint(this.data);

        verify(this.placementClient).createOpenstackTraits(this.newTraitsCaptor.capture());
        assertEquals(new TreeSet<>(NEW_TRAITS_ON_OS), new TreeSet<>(this.newTraitsCaptor.getValue()));

        verify(this.placementClient, times(2)).mapResourceProviderTraits(this.rpTraitsCaptor.capture());

        for (ResourceProviderTraits rpt : this.rpTraitsCaptor.getAllValues()) {
            if (rpt.getUuid().equals(HOST1_ID)) {
                TreeSet<String> treeSet = new TreeSet<>(HOST1_CIT_TAGS);
                treeSet.add(STANDARD_TRAIT_XX);
                assertEquals(new TreeSet<>(rpt.getTraits()), treeSet);
            } else {
                TreeSet<String> treeSet = new TreeSet<>(HOST2_CIT_TAGS);
                treeSet.add(STANDARD_TRAIT_YY);
                assertEquals(new TreeSet<>(rpt.getTraits()), treeSet);
            }
        }
    }

    @Test
    public void testSendDataToEndpoint_WithTrustedHostAndStaleTags_ExpectSuccess()
            throws AttestationHubException, RetryPlacementCallException, NetworkIOException {

        HashSet<String> standardTraits = Sets.newHashSet(STANDARD_TRAIT_XX, STANDARD_TRAIT_YY);
        doReturn(standardTraits)
        .doReturn(Sets.union(NEW_TRAITS_ON_OS, standardTraits))
        .when(this.placementClient).getOpenstackTraits();

        doReturn(new ResourceProviderTraits(HOST1_ID, 0, Sets.newHashSet(STANDARD_TRAIT_XX)))
        .doReturn(new ResourceProviderTraits(HOST1_ID, 0, Sets.union(HOST1_CIT_TAGS, Sets.newHashSet(STANDARD_TRAIT_XX))))
        .when(this.placementClient).getResourceProviderTraits(HOST1_ID);

        doReturn(new ResourceProviderTraits(HOST2_ID, 0, Sets.newHashSet(STANDARD_TRAIT_YY)))
        .doReturn(new ResourceProviderTraits(HOST2_ID, 0, Sets.union(HOST2_CIT_TAGS, Sets.newHashSet(STANDARD_TRAIT_YY))))
        .when(this.placementClient).getResourceProviderTraits(HOST2_ID);

        // First call
        this.novaClient.sendDataToEndpoint(this.data);

        // Update host 1 details, host 2 details remain the same
        this.data.hostDetailsList.set(0, generateHostDetails(
                TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags_update.json"), HOSTNAME1));

        // Second call, which include host1 traits which are stale and new traits as well
        this.novaClient.sendDataToEndpoint(this.data);

        verify(this.placementClient, times(2)).createOpenstackTraits(this.newTraitsCaptor.capture());

        // 3 mappings, 2 initial and 1 stale tag mapping
        verify(this.placementClient, times(3)).mapResourceProviderTraits(this.rpTraitsCaptor.capture());

        boolean host1FirstCallComplete = false;
        for (ResourceProviderTraits rpt : this.rpTraitsCaptor.getAllValues()) {
            if (rpt.getUuid().equals(HOST1_ID)) {
                if (!host1FirstCallComplete) {
                    TreeSet<String> treeSet = new TreeSet<>(HOST1_CIT_TAGS);
                    treeSet.add(STANDARD_TRAIT_XX);
                    assertEquals(new TreeSet<>(rpt.getTraits()), treeSet);
                    host1FirstCallComplete=true;
                } else {
                    // Second host1 call with updated tags i.e city updated
                    TreeSet<String> treeSet = new TreeSet<>(HOST1_CIT_UPDATED_TAGS);
                    treeSet.add(STANDARD_TRAIT_XX);
                    assertEquals(new TreeSet<>(rpt.getTraits()), treeSet);
                }
            } else {
                TreeSet<String> treeSet = new TreeSet<>(HOST2_CIT_TAGS);
                treeSet.add(STANDARD_TRAIT_YY);
                assertEquals(new TreeSet<>(rpt.getTraits()), treeSet);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendDataToEndpoint_WithConflictsRetry_ExpectSuccess()
            throws AttestationHubException, RetryPlacementCallException, NetworkIOException {

        doThrow(RetryPlacementCallException.class, RetryPlacementCallException.class).doNothing()
                .when(this.placementClient)
                .mapResourceProviderTraits(argThat(new ResourceProviderTraitsMatcher(HOST1_ID)));

        // Assuming the host1 traits are updated when conflict is encountered, so we add YY traits for next calls
        doReturn(new ResourceProviderTraits(HOST1_ID, 0, Sets.newHashSet(STANDARD_TRAIT_XX)))
                .doReturn(new ResourceProviderTraits(HOST1_ID, 0, Sets.newHashSet(STANDARD_TRAIT_XX, STANDARD_TRAIT_YY)))
                .doReturn(new ResourceProviderTraits(HOST1_ID, 0, Sets.newHashSet(STANDARD_TRAIT_XX, STANDARD_TRAIT_YY)))
                .when(this.placementClient).getResourceProviderTraits(HOST1_ID);

        this.novaClient.sendDataToEndpoint(this.data);

        // 2 successful and 2 failed calls(for host1)
        verify(this.placementClient, times(4)).mapResourceProviderTraits(this.rpTraitsCaptor.capture());

        List<ResourceProviderTraits> allValues = this.rpTraitsCaptor.getAllValues();
        boolean traitXXfound = false;

        assertEquals(4, allValues.size());
        for (ResourceProviderTraits rpt : allValues) {

            if (rpt.getUuid().equals(HOST1_ID)) {
                TreeSet<String> xxtreeSet = new TreeSet<>(HOST1_CIT_TAGS);
                xxtreeSet.add(STANDARD_TRAIT_XX);
                TreeSet<String> xxyytreeSet = new TreeSet<>(HOST1_CIT_TAGS);
                xxyytreeSet.add(STANDARD_TRAIT_XX);
                xxyytreeSet.add(STANDARD_TRAIT_YY);
                if (!traitXXfound) {
                    // First call contains only XX
                    assertEquals(new TreeSet<>(rpt.getTraits()), xxtreeSet);
                    traitXXfound = true;
                } else {
                    // Second and 3rd call contain XX and YY
                    assertEquals(new TreeSet<>(rpt.getTraits()), xxyytreeSet);
                }
            } else {
                TreeSet<String> treeSet = new TreeSet<>(HOST2_CIT_TAGS);
                treeSet.add(STANDARD_TRAIT_YY);
                assertEquals(new TreeSet<>(rpt.getTraits()), treeSet);
            }
        }
    }

    /**
     * Tests that for hosts where mapping failed for few hosts does not cause entire batch to fail.
     * @throws NetworkIOException
     */
    @Test
    public void testSendDataToEndpoint_WithErrors_ExpectPartialSuccess()
            throws AttestationHubException, RetryPlacementCallException, NetworkIOException {

        // 5 hosts in total
        this.data.hostDetailsList
        .addAll(generateHostDetails(TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags.json"), 2));
        this.data.hostDetailsList
        .add(generateHostDetails(TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags.json"), HOSTNAME3));

        // Throw exception even before attempting to map for additional hosts
        doThrow(AttestationHubException.class).when(this.placementClient)
                .getResourceProvider(ArgumentMatchers.startsWith(ADDITIONAL_HOSTNAME_PREFIX));

        doReturn(new ResourceProvider(HOST3_ID, 0)).when(this.placementClient).getResourceProvider(HOSTNAME3);
        doReturn(new ResourceProviderTraits(HOST3_ID, 0, Sets.newHashSet(STANDARD_TRAIT_XX))).when(this.placementClient)
        .getResourceProviderTraits(HOST3_ID);

        // Throw exception for all hosts except host3
        doThrow(AttestationHubException.class).when(this.placementClient)
                .mapResourceProviderTraits(argThat(new ResourceProviderTraitsMatcher(HOST3_ID, true)));

        try {
            this.novaClient.sendDataToEndpoint(this.data);
            fail("Expected AttestationHub Exception to be thrown");
        } catch (AttestationHubException ae) {
            verify(this.placementClient, times(3)).mapResourceProviderTraits(this.rpTraitsCaptor.capture());
            List<ResourceProviderTraits> allValues = this.rpTraitsCaptor.getAllValues();

            assertEquals(3, allValues.size());
            boolean host3TraitsMapped = this.rpTraitsCaptor.getAllValues().stream()
                    .anyMatch(rpt -> rpt.getUuid().equals(HOST3_ID));

            assertTrue("Expected host3 traits to be mapped", host3TraitsMapped);

        }
    }

    /**
     * Tests that for hosts where mapping failed for Consecutive hosts DOES cause entire batch to fail as it exceeds the
     * error threshold
     *
     * @throws NetworkIOException
     */
    @Test
    public void testSendDataToEndpoint_WithConsecutiveErrors_ExpectFailure()
            throws AttestationHubException, RetryPlacementCallException, NetworkIOException {

        // 2 + 5 = 7 hosts in total
        this.data.hostDetailsList
                .addAll(generateHostDetails(TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags.json"),
                        Constants.NO_OF_FAILED_CALLS_BEFORE_FAILING_BATCH));

        // Throw exception even before attempting to map for all additional hosts
        doThrow(NetworkIOException.class).when(this.placementClient)
                .getResourceProvider(ArgumentMatchers.startsWith(ADDITIONAL_HOSTNAME_PREFIX));

        // Throw exception for host1 and host2 as well
        doThrow(NetworkIOException.class).when(this.placementClient).mapResourceProviderTraits(any());

        try {
            this.novaClient.sendDataToEndpoint(this.data);
            fail("Expected AttestationHub Exception to be thrown");
        } catch (AttestationHubException ae) {
            assertEquals("Too many network errors encountered for the current batch of hosts. Failing the entire batch.",
                    ae.getMessage());
        }
    }

    @Test
    public void testSendDataToEndpoint_WithNonConsecutiveErrors_ExpectSuccess()
            throws AttestationHubException, RetryPlacementCallException, NetworkIOException {

        // 2(success) + 5(fail) + 1(success) + 5(fail) = 13 hosts in total
        this.data.hostDetailsList
        .addAll(generateHostDetails(TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags.json"),
               5, "fail-host-batch-1"));

        this.data.hostDetailsList
        .add(generateHostDetails(TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags.json"), HOSTNAME3));

        this.data.hostDetailsList
        .addAll(generateHostDetails(TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags.json"),
               5, "fail-host-batch-2"));

        // Throw exception even before attempting to map for all additional hosts
        doThrow(NetworkIOException.class).when(this.placementClient)
                .getResourceProvider(ArgumentMatchers.startsWith("fail-host"));

        doReturn(new ResourceProvider(HOST3_ID, 0)).when(this.placementClient).getResourceProvider(HOSTNAME3);
        doReturn(new ResourceProviderTraits(HOST3_ID, 0, Sets.newHashSet())).when(this.placementClient)
                .getResourceProviderTraits(HOST3_ID);

        try {
            this.novaClient.sendDataToEndpoint(this.data);
            fail("Expected AttestationHub Exception to be thrown");
        } catch (AttestationHubException ae) {
            assertEquals("Unable to map a few hosts with their asset tags, check log for more details",
                    ae.getMessage());
        }
    }

    @Test
    public void testSendDataToEndpoint_WithErrors_ExpectSuccess()
            throws AttestationHubException, NetworkIOException, RetryPlacementCallException {

        // 2 + 5 = 7 hosts in total
        this.data.hostDetailsList
                .addAll(generateHostDetails(TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags.json"),
                        Constants.NO_OF_FAILED_CALLS_BEFORE_FAILING_BATCH));

        // If host1,host2, host3 are not returning an error, we dont reach the error threshold, so batch should pass
        this.data.hostDetailsList
        .add(generateHostDetails(TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags.json"), HOSTNAME3));

        doReturn(new ResourceProvider(HOST3_ID, 0)).when(this.placementClient).getResourceProvider(HOSTNAME3);
        doReturn(new ResourceProviderTraits(HOST3_ID, 0, Sets.newHashSet())).when(this.placementClient)
                .getResourceProviderTraits(HOST3_ID);

        // Throw exception even before attempting to map
        doThrow(NetworkIOException.class).when(this.placementClient)
                .getResourceProvider(ArgumentMatchers.startsWith(ADDITIONAL_HOSTNAME_PREFIX));

        // do nothing for hosts making to this point(host1, host2, host3)
        doNothing().when(this.placementClient).mapResourceProviderTraits(any());

        try {
            this.novaClient.sendDataToEndpoint(this.data);
            fail("Expected AttestationHub Exception to be thrown");
        } catch (AttestationHubException ae) {
            assertNotEquals(
                    "Too many network errors encountered for the current batch of hosts. Failing the entire batch.",
                    ae.getMessage());
        }
    }

    @Test
    public void testSendDataToEndpoint_WithUnTrustedHostAndTags_ExpectSuccess()
            throws AttestationHubException, RetryPlacementCallException, NetworkIOException {
        this.data = new PublishData();
        this.data.hostDetailsList = new ArrayList<>();
        this.data.hostDetailsList
                .add(generateHostDetails(TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags.json"), HOSTNAME1));

        HashSet<String> standardTraits = Sets.newHashSet(STANDARD_TRAIT_XX, STANDARD_TRAIT_YY);
        doReturn(standardTraits)
        .doReturn(Sets.union(HOST1_CIT_TAGS, standardTraits))
        .when(this.placementClient).getOpenstackTraits();

        doReturn(new ResourceProviderTraits(HOST1_ID, 0, Sets.newHashSet(STANDARD_TRAIT_XX)))
        .doReturn(new ResourceProviderTraits(HOST1_ID, 0, Sets.union(HOST1_CIT_TAGS, Sets.newHashSet(STANDARD_TRAIT_XX))))
        .when(this.placementClient).getResourceProviderTraits(HOST1_ID);

        // First call
        this.novaClient.sendDataToEndpoint(this.data);

        // Update host 1 to be untrusted
        this.data.hostDetailsList.set(0,
                generateHostDetails(TestUtils.loadJsonData(TRUST_FOLDER + "untrusted_normal_tags.json"), HOSTNAME1));

        // Second call, which include untrusted host1
        this.novaClient.sendDataToEndpoint(this.data);

        verify(this.placementClient, times(2)).createOpenstackTraits(this.newTraitsCaptor.capture());

        verify(this.placementClient, times(2)).mapResourceProviderTraits(this.rpTraitsCaptor.capture());

        boolean host1FirstCallComplete = false;
        for (ResourceProviderTraits rpt : this.rpTraitsCaptor.getAllValues()) {
            if (!host1FirstCallComplete) {
                TreeSet<String> treeSet = new TreeSet<>(HOST1_CIT_TAGS);
                treeSet.add(STANDARD_TRAIT_XX);
                assertEquals(new TreeSet<>(rpt.getTraits()), treeSet);
                host1FirstCallComplete = true;
            } else {
                // Second host1 call with updated tags i.e no cit tags since its untrusted
                TreeSet<String> treeSet = new TreeSet<>();
                treeSet.add(STANDARD_TRAIT_XX);
                assertEquals(new TreeSet<>(rpt.getTraits()), treeSet);
            }
        }
    }

    @Test
    public void testGenerateTraitsFromAssetTags_WithTrustedHostAndTags_ExpectSuccess() throws AttestationHubException {
        String trustReportJson = TestUtils.loadJsonData(TRUST_FOLDER + "trusted_normal_tags.json");
        HostDetails hostDetails = generateHostDetails(trustReportJson);

        Set<String> actualSet = this.novaClient.generateTraitsFromTrustReport(hostDetails);

        assertEquals(new TreeSet<>(HOST1_CIT_TAGS), new TreeSet<>(actualSet));
    }

    @Test
    public void testGenerateTraitsFromAssetTags_WithUnTrustedHostAndTags_ExpectSuccess() throws AttestationHubException {
        String trustReportJson = TestUtils.loadJsonData(TRUST_FOLDER + "untrusted_normal_tags.json");
        HostDetails hostDetails = generateHostDetails(trustReportJson);
        Set<String> expectedSet = Collections.emptySet();

        Set<String> actualSet = this.novaClient.generateTraitsFromTrustReport(hostDetails);

        assertEquals(new TreeSet<>(expectedSet), new TreeSet<>(actualSet));
    }

    @Test
    public void testGenerateTraitsFromAssetTags_WithSpecialCharInTags_ExpectSuccess() throws AttestationHubException {
        String trustReportJson = TestUtils.loadJsonData(TRUST_FOLDER + "trusted_special_char_tags.json");
        HostDetails hostDetails = generateHostDetails(trustReportJson);
        Set<String> expectedSet = Sets.newHashSet("ISECL_AT_COUNTRY___US", "ISECL_AT_STATE__O_R",
                "ISECL_AT_STATE___TX", "ISECL_AT_STATE__A_Z", "ISECL_TRUSTED",
                "ISECL_AT_CITY___AUS_TIN", "ISECL_AT_CITY_____HILLSBORO",
                "ISECL_AT_FOO_BAR_ZZZ", "ISECL_AT_FOO__BAR_ZZZ", "ISECL_AT_FOO____BAR___Z__Z");

        Set<String> actualSet = this.novaClient.generateTraitsFromTrustReport(hostDetails);

        assertEquals(new TreeSet<>(expectedSet), new TreeSet<>(actualSet));
    }

    @Test
    public void testGenerateTraitsFromAssetTags_WithNoTags_ExpectSuccess() throws AttestationHubException {
        String trustReportJson = TestUtils.loadJsonData(TRUST_FOLDER + "trusted_no_tags.json");
        HostDetails hostDetails = generateHostDetails(trustReportJson);
        Set<String> expectedSet = Sets.newHashSet("ISECL_TRUSTED");

        Set<String> actualSet = this.novaClient.generateTraitsFromTrustReport(hostDetails);

        assertEquals(expectedSet, actualSet);
    }

    @Test
    public void testGenerateTraitsFromAssetTags_NoData_ExpectSuccess() throws AttestationHubException {
        HostDetails hostDetails = generateHostDetails("{}");
        Set<String> expectedSet = new HashSet<>();

        Set<String> actualSet = this.novaClient.generateTraitsFromTrustReport(hostDetails);

        assertEquals(expectedSet, actualSet);
    }

    private HostDetails generateHostDetails(String trustReportJson) {
        return generateHostDetails(trustReportJson, null);
    }

    private List<HostDetails> generateHostDetails(String trustReportJson, int additionalHosts) {
        return generateHostDetails(trustReportJson, additionalHosts, ADDITIONAL_HOSTNAME_PREFIX);
    }

    private List<HostDetails> generateHostDetails(String trustReportJson, int additionalHosts, String hostPrefix) {
        List<HostDetails> additionalHostsList = new ArrayList<>();
        int noOfPreconfiguredHost = 3;
        for(int i=0;i<additionalHosts; i++) {
            additionalHostsList
                    .add(generateHostDetails(trustReportJson, hostPrefix + ++noOfPreconfiguredHost));
        }
        return additionalHostsList;
    }

    private HostDetails generateHostDetails(String trustReportJson, String hostName) {
        HostDetails hostDetails = new HostDetails();
        hostDetails.trust_report = trustReportJson;
        hostDetails.hostname = hostName;
        return hostDetails;
    }

    private static class ResourceProviderTraitsMatcher implements ArgumentMatcher<ResourceProviderTraits> {

        private List<String> rpIds = new ArrayList<>();
        private boolean doesNotMatch;

        public ResourceProviderTraitsMatcher(String rpId) {
            this(rpId, false);
        }

        public ResourceProviderTraitsMatcher(String rpId, boolean doesNotMatch) {
            this.rpIds.add(rpId);
            this.doesNotMatch = doesNotMatch;
        }

        @Override
        public boolean matches(ResourceProviderTraits argument) {
            boolean matches = this.rpIds.contains(argument.getUuid());
            return this.doesNotMatch ? !matches : matches;
        }

    }

}
