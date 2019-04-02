/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.attestationhub.plugin.nova.TestUtils;
import com.intel.attestationhub.plugin.nova.identity.model.AuthResponseV3;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class V3IdentityServiceImplTest {

    private static final String IDENTITY_FOLDER = "identity" + File.separator;
    private V3IdentityServiceImpl identityServiceV3;

    @Before
    public void setup() {
        this.identityServiceV3 = new V3IdentityServiceImpl();
    }

    @Test
    public void testGetAuthRequestBody() throws AttestationHubException, IOException {
        String expectedAuthBody = TestUtils.loadJsonData(IDENTITY_FOLDER + "v3AuthRequest.json");
        String authRequestBody = this.identityServiceV3.getAuthRequestBody("domain", "project", "user", "password");
        assertEquals(expectedAuthBody.replaceAll("\\s", ""), authRequestBody);
    }

    @Test
    public void testParseResponse() throws AttestationHubException, IOException {

        String response = TestUtils.loadJsonData(IDENTITY_FOLDER + "v3AuthResponse.json");
        ObjectMapper mapper = new ObjectMapper();

        AuthResponseV3 authResponse = mapper.readValue(response, AuthResponseV3.class);

        assertNotNull("Auth Response Null!!", authResponse);
        assertEquals("Keystone URL does not match expected output", "http://192.168.0.1/identity",
                authResponse.getEndpointUrl("identity"));
        assertEquals("Placement URL does not match expected output", "http://192.168.0.1/placement",
                authResponse.getEndpointUrl("placement"));

    }

}
