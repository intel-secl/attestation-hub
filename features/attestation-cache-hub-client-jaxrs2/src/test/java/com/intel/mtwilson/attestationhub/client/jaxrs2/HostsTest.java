/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.attestationhub.client.jaxrs2;

import com.intel.attestationhub.api.HostFilterCriteria;
import com.intel.dcsg.cpg.extensions.Extensions;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateDigestTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyCreator;
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
public class HostsTest {
    Properties properties = new Properties();
    
    public HostsTest() {
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
    public void testRetrieveHosts() throws Exception {
       // Create the client and call the retrieve API
       Hosts client = new Hosts(properties);
       Response result = client.retrieve("2EA3F28E-BA8E-4350-811D-353086E5E8CF");

       System.out.println("result: " + result.toString());
       System.out.println("result: " + result.readEntity(String.class));
    }

    @Test
    public void testSearchHosts() throws Exception {
        HostFilterCriteria filterCriteria = new HostFilterCriteria();
        filterCriteria.nameEqualTo = "RHEL-1";
        
        // Create the client and call the search API
        Hosts client = new Hosts(properties);
        Response result = client.search(filterCriteria);

       System.out.println("result: " + result.toString());
       System.out.println("result: " + result.readEntity(String.class));
    }
}
