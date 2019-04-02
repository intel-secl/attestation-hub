/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.attestationhub.client.jaxrs2;

import com.intel.attestationhub.api.Mapping;
import com.intel.dcsg.cpg.extensions.Extensions;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateDigestTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyCreator;
import java.util.Arrays;
import java.util.Properties;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author purvades
 */
public class HostAssignmentsTest {
    Properties properties = new Properties();
    
    public HostAssignmentsTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        Extensions.register(TlsPolicyCreator.class, CertificateDigestTlsPolicyCreator.class);
        Extensions.register(TlsPolicyCreator.class, CertificateTlsPolicyCreator.class);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        properties.put("mtwilson.api.baseurl", "https://192.168.0.1:19445/v1");
        properties.put("mtwilson.api.username", "hubadmin");
        properties.put("mtwilson.api.password", "HubPassword"); 
        properties.put("mtwilson.api.tls.policy.certificate.sha256", "db7d1de5690ebdbeab40875b5cf91ba0b08cf0ef7271d7efbc4cd8d6c36f299d");
    }
    
    @After
    public void tearDown() {
    }

    @Test
     public void testCreateHostAssignments() throws Exception {
        Mapping mapping = new Mapping();
        mapping.tenant_id = "72B99FA9-8FBB-4F20-B988-3990EB4410DA";
        mapping.hardware_uuids = Arrays.asList("97a65f4e-62ed-479b-9e4e-efa143ac5d5e", "a8f024fc-ebcd-40f3-8ba9-6be4bf6ecb9c");

        // Create the client and call the create API
        HostAssignments client = new HostAssignments(properties);
        Response result = client.create(mapping);
        
        System.out.println("result: " + result.toString());
        System.out.println("result: " + result.readEntity(String.class));
     }
}
