/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.attestationhub.client.jaxrs2;

import com.intel.attestationhub.api.Tenant;
import com.intel.attestationhub.api.TenantFilterCriteria;
import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.mtwilson.jaxrs2.client.MtWilsonClient;
import java.util.HashMap;
import java.util.Properties;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * These resources are used to manage tenants in attestation hub.
 * <pre>
 * A “tenant” in the context of the Integration Hub is an endpoint to which 
 * Attestation Report data will be pushed.  By mapping Hosts to Tenants, the 
 * Integration Hub provides multitenancy support, allowing Tenant A to receive 
 * Attestation Reports only for Hosts actually assigned to Tenant A.  When a Tenant 
 * object is defined for the Integration Hub, connectivity details are provided that 
 * allow the Hub to push the relevant information to an integration endpoint, 
 * such as OpenStack Nova.
 * At least one tenant must be created to receive the attestations.
 * </pre>
 */
public class Tenants extends MtWilsonClient {

    Logger log = LoggerFactory.getLogger(getClass().getName());

    /**
     * Constructor.
     * 
     * @param properties This java properties model must include server connection details for the API client initialization.
     * <pre>
     * mtwilson.api.baseurl - Integration Hub base URL for accessing REST APIs
     * 
     * // basic authentication
     * mtwilson.api.username - Username for API basic authentication with the Hub
     * mtwilson.api.password - Password for API basic authentication with the Hub
     * 
     * <b>Example:</b>
     * Properties properties = new Properties();
     * 
     * properties.put(“mtwilson.api.baseurl”, “https://hub.server.com:19445/v1”);
     * // basic authentication
     * properties.put(“mtwilson.api.username”, “user”);
     * properties.put(“mtwilson.api.password”, “*****”);
     * properties.put("mtwilson.api.tls.policy.certificate.sha256", "db7d1de5690ebdbeab40875b5cf91ba0b08cf0ef7271d7efbc4cd8d6c36f299d");
     * Tenants client = new Tenants(properties);
     * </pre>
     * @throws Exception 
     */
    public Tenants(Properties properties) throws Exception {
        super(properties);
    }
    
    /**
     * Creates a tenant.
     * <pre>
     * It is created by passing the JSON that configures the tenant and its plugins. The user needs to provide 
     * the plugin.provider configuration for the plugin. It tells the hub the name of the connector class that 
     * would push the data to the endpoint. This will keep the Hub not tied to any defined set of connectors/plugins.
     * </pre>
     * @param tenant The serialized Tenant java model object represents the content of the request body.
     * <pre>
     *          name (required)               Name of the tenant to be created.
     * 
     *          plugins (required)            List of plugins with following information.
     *                                        This information needs to be in correct JSON format.
     * 
     *                                        name              name of plugin
     *                                                          Plugin name has to be either of nova/kubernetes/mesos else
     *                                                          the code will throw validation error.
     * 
     *                                        properties        properties contains key value pair of configuration details of openstack server
     * 
     *                                        In case of auth version = v3, domain.name needs to configured too.
     * 
     * </pre>
     * @return <pre>The serialized Response java model object that was created:
     *          id
     *          name
     *          plugins</pre>
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON
     * @mtwMethodType POST
     * @mtwSampleRestCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * https://hub.server.com:19445/v1/tenants
     * 
     * Input:
     * {
     *     "name": "COKE",
     *     "plugins": [{
     *         "name": "nova",
     *         "properties": [
     *             {
     *             "key": "api.endpoint",
     *             "value": "http://openstack.server.com:8774"
     *             },
     *             {
     *                 "key": "auth.endpoint",
     *                 "value": "http://openstack.server.com:5000"
     *             },
     *             {
     *                 "key": "auth.version",
     *                 "value": "v2"
     *             },
     *             {
     *                 "key": "user.name",
     *                 "value": "admin"
     *             },
     *             {
     *                 "key": "user.password",
     *                 "value": "password"
     *             },
     *             {
     *                 "key": "tenant.name",
     *                 "value": "default"
     *             },
     *             {
     *                  "key": "plugin.provider",
     *                  "value": "com.intel.attestationhub.plugin.nova.NovaPluginImpl"
     *             } 
     *         ]
     *     }]
     * }
     * 
     * Output:
     * {
     *     "id": "BA49C7C8-B092-4841-A747-D4F4084AE5B8"
     *     "name": "COKE",
     *     "plugins": [{
     *         "name": "nova",
     *         "properties": [
     *             {
     *             "key": "api.endpoint",
     *             "value": "http://openstack.server.com:8774"
     *             },
     *             {
     *                 "key": "auth.endpoint",
     *                 "value": "http://openstack.server.com:5000"
     *             },
     *             {
     *                 "key": "auth.version",
     *                 "value": "v2"
     *             },
     *             {
     *                 "key": "user.name",
     *                 "value": "admin"
     *             },
     *             {
     *                 "key": "user.password",
     *                 "value": "password"
     *             },
     *             {
     *                 "key": "tenant.name",
     *                 "value": "default"
     *             },
     *             {
     *                  "key": "plugin.provider",
     *                  "value": "com.intel.attestationhub.plugin.nova.NovaPluginImpl"
     *             } 
     *         ]
     *     }]
     * }
     * </div></pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * // Create tenant model object and set the name and then plugins innformation
     * Tenant tenant = new Tenant();
     * tenant.setName("COKE");
     * 
     * // Create list of plugins
     * List<Plugin> plugins = new ArrayList<>();
     *   
     * // Create the first plugin with it's name and properties
     * Plugin plugin = new Plugin();
     * plugin.setName("nova");
     * 
     * // Create list of properties
     * List<PluginProperty> prop = new ArrayList<>();
     * 
     * // Adding all the required properties
     * PluginProperty property = new PluginProperty();
     * property.setKey("api.endpoint");
     * property.setValue("http://openstack.server.com:8774");
     * prop.add(property);
     * 
     * property.setKey("auth.endpoint");
     * property.setValue("http://openstack.server.com:5000");
     * prop.add(property);
     * 
     * property.setKey("auth.version");
     * property.setValue("v2");
     * prop.add(property);
     * 
     * property.setKey("user.name");
     * property.setValue("admin");
     * prop.add(property);
     * 
     * property.setKey("user.password");
     * property.setValue("password");
     * prop.add(property);
     * 
     * property.setKey("tenant.name");
     * property.setValue("default");
     * prop.add(property);
     * 
     * property.setKey("plugin.provider");
     * property.setValue("com.intel.attestationhub.plugin.nova.NovaPluginImpl");
     * prop.add(property);
     * 
     * // Set created properties in plugin
     * plugin.setProperties(prop);
     * plugins.add(plugin);
     * 
     * // Set plugins in tenant
     * tenant.setPlugins(plugins);
     * 
     * // Create the client and call the create API
     * Tenants client = new Tenants(properties);
     * Response reponse = client.create(tenant);
     * </pre></div>
     */
    public Response create(Tenant tenant) {
        log.debug("target: {}", getTarget().getUri().toString());
        Response response = getTarget().path("tenants").request().accept(MediaType.APPLICATION_JSON).header("Authorization", "Token ").post(Entity.json(tenant), Response.class);
        return response;
    }
    
    /**
     * Retrieves a tenant configuration.
     * @param id ID of the tenant specified as a path parameter.
     * @return <pre>The serialized Response java model object that was retrieved:
     *          id
     *          name
     *          plugins</pre>
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * https://hub.server.com:19445/v1/tenants/BA49C7C8-B092-4841-A747-D4F4084AE5B8 
     * 
     * Output:
     * {
     *     "id": "BA49C7C8-B092-4841-A747-D4F4084AE5B8",
     *     "name": "COKE",
     *     "plugins": [{
     *         "name": "nova",
     *         "properties": [
     *             {
     *             "key": "api.endpoint",
     *             "value": "http://openstack.server.com:8774"
     *             },
     *             {
     *                 "key": "auth.endpoint",
     *                 "value": "http://openstack.server.com:5000"
     *             },
     *             {
     *                 "key": "auth.version",
     *                 "value": "v2"
     *             },
     *             {
     *                 "key": "user.name",
     *                 "value": "admin"
     *             },
     *             {
     *                 "key": "user.password",
     *                 "value": "password"
     *             },
     *             {
     *                 "key": "tenant.name",
     *                 "value": "default"
     *             },
     *             {
     *                  "key": "plugin.provider",
     *                  "value": "com.intel.attestationhub.plugin.nova.NovaPluginImpl"
     *             } 
     *         ]
     *     }]
     * }
     * </pre></div>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * // Create the client and call the retrieve API
     * Tenants client = new Tenants(properties);
     * Response reponse = client.retrieve("BA49C7C8-B092-4841-A747-D4F4084AE5B8");
     * </pre></div>
     */
    public Response retrieve(String id) {
        log.debug("target: {}", getTarget().getUri().toString());
        HashMap<String,Object> map = new HashMap<>();
        map.put("id", id);
        Response response = getTarget().path("tenants/{id}").resolveTemplates(map).request(MediaType.APPLICATION_JSON).get(Response.class);
        return response;
    }
    
    /**
     * Updates a tenant configuration.
     * @param tenant The serialized Tenant java model object represents the content of the request body.
     * <pre>
     * Tenant ID needs to be provider in URL. If ID is incorrect the method will throw 
     * 602, Invalid ID error.
     * 
     *          name                          Name of the tenant to be created.
     *                                        If it's empty method will throw 600, Validation error.
     * 
     *          plugins                       List of plugins with following information.
     *                                        This information needs to be in correct JSON format.
     * 
     *                                        name              name of plugin.
     *                                                          Plugin name has to be either of nova/kubernetes/mesos else
     *                                                          the code will throw validation error.
     * 
     *                                        properties        properties contains key value pair of configuration details of openstack server.
     * 
     *                                        In case of auth version = v3, domain.name needs to configured too.
     * 
     * </pre>
     * @return <pre>The serialized Response java model object that was updated:
     *          id
     *          name
     *          plugins</pre>
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON
     * @mtwMethodType PUT
     * @mtwSampleRestCall
     * <pre>
     * https://hub.server.com:19445/v1/tenants/BA49C7C8-B092-4841-A747-D4F4084AE5B8 
     * 
     * Input:
     * {
     *     "name": "COKE",
     *     "plugins": [{
     *         "name": "nova",
     *         "properties": [
     *             {
     *             "key": "api.endpoint",
     *             "value": "http://openstack.server.com:8774"
     *             },
     *             {
     *                 "key": "auth.endpoint",
     *                 "value": "http://openstack.server.com:5000"
     *             },
     *             {
     *                 "key": "auth.version",
     *                 "value": "v2"
     *             },
     *             {
     *                 "key": "user.name",
     *                 "value": "admin"
     *             },
     *             {
     *                 "key": "user.password",
     *                 "value": "password"
     *             },
     *             {
     *                 "key": "tenant.name",
     *                 "value": "default"
     *             },
     *             {
     *                  "key": "plugin.provider",
     *                  "value": "com.intel.attestationhub.plugin.nova.NovaPluginImpl"
     *             } 
     *         ]
     *     }]
     * }
     * 
     * Output:
     * {
     *     "id": "BA49C7C8-B092-4841-A747-D4F4084AE5B8",
     *     "name": "COKE",
     *     "plugins": [{
     *         "name": "nova",
     *         "properties": [
     *             {
     *             "key": "api.endpoint",
     *             "value": "http://openstack.server.com:8774"
     *             },
     *             {
     *                 "key": "auth.endpoint",
     *                 "value": "http://openstack.server.com:5000"
     *             },
     *             {
     *                 "key": "auth.version",
     *                 "value": "v2"
     *             },
     *             {
     *                 "key": "user.name",
     *                 "value": "admin"
     *             },
     *             {
     *                 "key": "user.password",
     *                 "value": "password"
     *             },
     *             {
     *                 "key": "tenant.name",
     *                 "value": "default"
     *             },
     *             {
     *                  "key": "plugin.provider",
     *                  "value": "com.intel.attestationhub.plugin.nova.NovaPluginImpl"
     *             } 
     *         ]
     *     }]
     * }
     * </pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * // Create tenant model object and set the name and then plugins innformation
     * Tenant tenant = new Tenant();
     * tenant.setName("COKE");
     * 
     * // Create list of plugins
     * List<Plugin> plugins = new ArrayList<>();
     *   
     * // Create the first plugin with it's name and properties
     * Plugin plugin = new Plugin();
     * plugin.setName("nova");
     * 
     * // Create list of properties
     * List<PluginProperty> prop = new ArrayList<>();
     * 
     * // Adding all the required properties
     * PluginProperty property = new PluginProperty();
     * property.setKey("api.endpoint");
     * property.setValue("http://openstack.server.com:8774");
     * prop.add(property);
     * 
     * property.setKey("auth.endpoint");
     * property.setValue("http://openstack.server.com:5000");
     * prop.add(property);
     * 
     * property.setKey("auth.version");
     * property.setValue("v2");
     * prop.add(property);
     * 
     * property.setKey("user.name");
     * property.setValue("admin");
     * prop.add(property);
     * 
     * property.setKey("user.password");
     * property.setValue("password");
     * prop.add(property);
     * 
     * property.setKey("tenant.name");
     * property.setValue("default");
     * prop.add(property);
     * 
     * property.setKey("plugin.provider");
     * property.setValue("com.intel.attestationhub.plugin.nova.NovaPluginImpl");
     * prop.add(property);
     * 
     * // Set created properties in plugin
     * plugin.setProperties(prop);
     * plugins.add(plugin);
     * 
     * // Set plugins in tenant
     * tenant.setPlugins(plugins);
     * 
     * // Create the client and call the update API
     * Tenants client = new Tenants(properties);
     * Response reponse = client.update(tenant);
     * </pre></div>
     */
    public Response update(Tenant tenant) {
        log.debug("target: {}", getTarget().getUri().toString());
        HashMap<String,Object> map = new HashMap<>();
        map.put("id", tenant.getId());
        Response response = getTarget().path("tenants").resolveTemplates(map).request().accept(MediaType.APPLICATION_JSON).put(Entity.json(tenant), Response.class);
        return response;
    }
    
    
    /**
     * Deletes a tenant configuration.
     * <pre>
     * If ID is not in proper format, it throws error 602, Invalid ID.
     * If there is failure on server side, it throws 601, Request processing failed.
     * </pre>
     * @param id ID of the tenant specified as a path parameter.
     * @return <pre>The serialized Response java model object that was deleted:
     *          id
     *          name
     *          plugins</pre>
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON
     * @mtwMethodType DELETE
     * @mtwSampleRestCall
     * <pre>
     * https://hub.server.com:19445/v1/tenants/BA49C7C8-B092-4841-A747-D4F4084AE5B8 
     * 
     * Output:
     * {
     *     "id": "BA49C7C8-B092-4841-A747-D4F4084AE5B8",
     *     "name": "COKE",
     *     "plugins": [{
     *         "name": "nova",
     *         "properties": [
     *             {
     *             "key": "api.endpoint",
     *             "value": "http://openstack.server.com:8774"
     *             },
     *             {
     *                 "key": "auth.endpoint",
     *                 "value": "http://openstack.server.com:5000"
     *             },
     *             {
     *                 "key": "auth.version",
     *                 "value": "v2"
     *             },
     *             {
     *                 "key": "user.name",
     *                 "value": "admin"
     *             },
     *             {
     *                 "key": "user.password",
     *                 "value": "password"
     *             },
     *             {
     *                 "key": "tenant.name",
     *                 "value": "default"
     *             },
     *             {
     *                  "key": "plugin.provider",
     *                  "value": "com.intel.attestationhub.plugin.nova.NovaPluginImpl"
     *             } 
     *         ]
     *     }]
     * }
     * </pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * // Create the client and call the delete API
     * Tenants client = new Tenants(properties);
     * Response reponse = client.delete("BA49C7C8-B092-4841-A747-D4F4084AE5B8");
     * </pre></div>
     */
    public Response delete(String id) {
        log.debug("target: {}", getTarget().getUri().toString());
        HashMap<String,Object> map = new HashMap<>();
        map.put("id", id);
        Response response = getTarget().path("tenants/{id}").resolveTemplates(map).request(MediaType.APPLICATION_JSON).delete(Response.class);
        return response;
    }
    
    /**
     * Searches for tenant configuration.
     * <pre>
     * Returns all tenants configuration if the search string is not provided
     * If there is failure on server side, it throws 601, Request processing failed.
     * </pre>
     * @param filterCriteria The content models of the TenantFilterCriteria java model object can be used as query parameters.
     * <pre>
     *          nameEqualTo (required)       Name of the tenant.
     * </pre>
     * @return <pre>The serialized Response java model object that was searched containing list of:
     *          id
     *          name
     *          plugins</pre>
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * <pre>
     * https://hub.server.com:19445/v1/tenants?nameEqualTo=COKE
     * 
     * Output:
     * {
     *     "id": "BA49C7C8-B092-4841-A747-D4F4084AE5B8",
     *     "name": "COKE",
     *     "plugins": [{
     *         "name": "nova",
     *         "properties": [
     *             {
     *             "key": "api.endpoint",
     *             "value": "http://openstack.server.com:8774"
     *             },
     *             {
     *                 "key": "auth.endpoint",
     *                 "value": "http://openstack.server.com:5000"
     *             },
     *             {
     *                 "key": "auth.version",
     *                 "value": "v2"
     *             },
     *             {
     *                 "key": "user.name",
     *                 "value": "admin"
     *             },
     *             {
     *                 "key": "user.password",
     *                 "value": "password"
     *             },
     *             {
     *                 "key": "tenant.name",
     *                 "value": "default"
     *             },
     *             {
     *                  "key": "plugin.provider",
     *                  "value": "com.intel.attestationhub.plugin.nova.NovaPluginImpl"
     *             } 
     *         ]
     *     }]
     * }
     * </pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * // Create the tenant filter criteria model and set the search criteria
     * TenantFilterCriteria filterCriteria = new TenantFilterCriteria();
     * filterCriteria.nameEqualTo = "COKE";
     * 
     * // Create the client and call the search API
     * Tenants client = new Tenants(properties);
     * Response reponse = client.search(filterCriteria);
     * </pre></div>
     */
    public Response search(TenantFilterCriteria filterCriteria) {
        log.debug("target: {}", getTarget().getUri().toString());
        Response response = getTargetPathWithQueryParams("tenants", filterCriteria).request(MediaType.APPLICATION_JSON).get(Response.class);
        return response;
    }
}
