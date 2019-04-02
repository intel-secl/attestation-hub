/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mtwilson.attestationhub.client.jaxrs2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.attestationhub.api.Tenant;
import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.attestationhub.api.Tenant.Property;
import com.intel.attestationhub.api.TenantFilterCriteria;
import com.intel.dcsg.cpg.extensions.Extensions;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateDigestTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyCreator;
import java.net.MalformedURLException;
import java.util.*;
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
public class TenantsTest {
    Properties properties = new Properties();
    
    public TenantsTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testRetrieveTenant() throws MalformedURLException, Exception {
       // Create the client and call the retrieve API
        List<Tenant.Property> credentials = new ArrayList<>();
        String decryptedCredential = "[{\"key\":\"user.name\",\"value\":\"admin\"},{\"key\":\"user.password\",\"value\":\"password\"}]";
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        List<Object> credentialInPlain = mapper.readValue(decryptedCredential, List.class);
        for(Object o : credentialInPlain) {
            LinkedHashMap l = (LinkedHashMap) o;
            List<String> keyValues = new ArrayList<>();
            for(Object entry: l.values()) {
                keyValues.add((String)entry);
            }
            Property p = getProperty(keyValues);
            credentials.add(p);
        }
        for (Tenant.Property p : credentials) {
            System.out.println("Tenant credentials are key - " + p.getKey() + " value - " + p.getValue());
        }
    }
    private Property getProperty(List<String> values) {
        return new Property(values.get(0), values.get(1));
    }

}
