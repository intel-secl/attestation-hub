/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.attestationhub.client.jaxrs2;

import com.intel.attestationhub.api.Tenant;
import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.attestationhub.api.Tenant.PluginProperty;
import com.intel.attestationhub.api.TenantFilterCriteria;
import com.intel.dcsg.cpg.extensions.Extensions;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateDigestTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyCreator;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
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
public class TenantsTest {
    Properties properties = new Properties();
    
    public TenantsTest() {
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
    public void testRetrieveTenant() throws MalformedURLException, Exception {
       // Create the client and call the retrieve API
       Tenants client = new Tenants(properties);
       Response result = client.retrieve("2EA3F28E-BA8E-4350-811D-353086E5E8CF");

       System.out.println("result: " + result.toString());
       System.out.println("result: " + result.readEntity(String.class));
    }

    @Test
    public void testSearchTenant() throws MalformedURLException, Exception {
       //Create a filter creiteria and set name as query parameter
       TenantFilterCriteria filterCriteria = new TenantFilterCriteria();
       filterCriteria.nameEqualTo = "COKE";

       // Create the client and call the search API
       Tenants client = new Tenants(properties);
       Response result = client.search(filterCriteria);

       System.out.println("result: " + result.toString());
       System.out.println("result: " + result.readEntity(String.class));
    }

    @Test
    public void testCreateTenant() throws MalformedURLException, Exception {
       Tenant tenant = new Tenant();
       tenant.setName("COKE");

       List<Plugin> plugins = new ArrayList<>();

       Plugin plugin = new Plugin();
       plugin.setName("nova");

       List<PluginProperty> prop = new ArrayList<>();

       PluginProperty property = new PluginProperty();
       property.setKey("api.endpoint");
       property.setValue("http://openstack.server.com:8774");
       prop.add(property);

       property.setKey("auth.endpoint");
       property.setValue("http://openstack.server.com:5000");
       prop.add(property);

       property.setKey("auth.version");
       property.setValue("v2");
       prop.add(property);

       property.setKey("user.name");
       property.setValue("admin");
       prop.add(property);

       property.setKey("user.password");
       property.setValue("password");
       prop.add(property);

       property.setKey("tenant.name");
       property.setValue("default");
       prop.add(property);

       property.setKey("plugin.provider");
       property.setValue("com.intel.attestationhub.plugin.nova.NovaPluginImpl");
       prop.add(property);

       plugin.setProperties(prop);
       plugins.add(plugin);

       tenant.setPlugins(plugins);

       // Create the client and call the create API
       Tenants client = new Tenants(properties);
       Response result = client.create(tenant);

       System.out.println("result: " + result.toString());
       System.out.println("result: " + result.readEntity(String.class));
    }

    @Test
    public void testDeleteTenant() throws MalformedURLException, Exception {
       // Create the client and call the search API
       Tenants client = new Tenants(properties);
       Response result = client.delete("0E81634F-3421-4AF5-8AAE-1C02F1B35CDB");

       System.out.println("result: " + result.toString());
       System.out.println("result: " + result.readEntity(String.class));
    }
}
