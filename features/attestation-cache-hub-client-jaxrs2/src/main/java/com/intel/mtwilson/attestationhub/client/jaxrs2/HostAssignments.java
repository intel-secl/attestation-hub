/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.attestationhub.client.jaxrs2;

import com.intel.attestationhub.api.Mapping;
import com.intel.attestationhub.api.SearchCriteriaForMapping;
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
 * These resources are used to manage the mapping of hosts and tenants.
 * <pre>
 * The Integration Hub acts as a middle-man between the Attestation Service and 
 * one or more scheduler services (such as OpenStack), and "pushes" attestation information 
 * retrieved from the Attestation Service to one or more scheduler services according 
 * to an assignment of hosts to specific tenants.
 * 
 * By mapping Hosts to Tenants, the Integration Hub provides multitenancy support and 
 * added security protection. It restricts tenants from viewing or pulling  host 
 * information and reports  that they are not authorized to view.  
 * 
 * In this way, tenants will receive attestation information for hosts that belong to 
 * that tenant and not others.
 * </pre>
 */
public class HostAssignments extends MtWilsonClient {

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
     * HostAssignments client = new HostAssignments(properties);
     * </pre>
     * @throws Exception 
     */
    public HostAssignments(Properties properties) throws Exception {
        super(properties);
    }
    
    /**
     * Creates tenant to host mappings.
     * @param mapping The serialized Mapping java model object represents the content of the request body.
     * <pre>
     *          tenant_id (required)             ID of the tenant.
     *                                           The method will throw 602 error if tenant ID is not in proper format.
     * 
     *          hardware_uuids (required)        List of id of the hosts to be mapped with this tenant.
     * </pre>
     * @return <pre>The serialized Response java model object that was created containing list of 'mappings' for 
     * provided hardware UUIDs to the tenant each containing:
     *          mapping_id
     *          tenant_id
     *          hardware_uuid</pre>
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON
     * @mtwMethodType POST
     * @mtwSampleRestCall
     * <pre>
     * https://hub.server.com:19445/v1/host-assignments
     * Input: 
     * {
     *      "tenant_id": "256E853E-41EA-4E44-B531-420C5CDB35B5",
     *      "hardware_uuids": ["97a65f4e-62ed-479b-9e4e-efa143ac5d5e", "a8f024fc-ebcd-40f3-8ba9-6be4bf6ecb9c"] 
     * }
     * 
     * Output: 
     * {
     *     "mappings": [{
     *       "mapping_id": "3FC59BE5-6352-4530-879C-A4DFDF085BD7",
     *       "tenant_id": "256E853E-41EA-4E44-B531-420C5CDB35B5",
     *       "hardware_uuid": "97a65f4e-62ed-479b-9e4e-efa143ac5d5e"
     *     },
     *     {
     *       "mapping_id": "3FB0B5E0-1066-4DED-BE03-A35BB909E9B8",
     *       "tenant_id": "256E853E-41EA-4E44-B531-420C5CDB35B5",
     *       "hardware_uuid": "a8f024fc-ebcd-40f3-8ba9-6be4bf6ecb9c"
     *     }]
     * }
     * </pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * // Create the mapping model and set the tenant ID and hardware UUIDs to be mapped
     * Mapping mapping = new Mapping();
     * mapping.tenant_id = "72B99FA9-8FBB-4F20-B988-3990EB4410DA";
     * mapping.hardware_uuids = Arrays.asList("97a65f4e-62ed-479b-9e4e-efa143ac5d5e", "a8f024fc-ebcd-40f3-8ba9-6be4bf6ecb9c");
     * 
     * // Create the client and call the create API
     * HostAssignments client = new HostAssignments(properties);
     * Response reponse = client.create(mapping);
     * </pre></div>
     */
    public Response create(Mapping mapping) {
        log.debug("target: {}", getTarget().getUri().toString());
        Response response = getTarget().path("host-assignments").request().accept(MediaType.APPLICATION_JSON).post(Entity.json(mapping), Response.class);
        return response;
    }
    
    /**
     * Retrieves tenant to host mapping information.
     * @param id ID of the mapping specified as a path parameter.
     * @return <pre>The serialized Response java model object that was retrieved:
     *          id
     *          host_hardware_uuid
     *          deleted
     *          tenant</pre>
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * <pre>
     * https://hub.server.com:19445/v1/host-assignments/3FB0B5E0-1066-4DED-BE03-A35BB909E9B8
     * 
     * Output: 
     * {
     *     "id": "26054082-8C79-4CEF-B1B5-3AA71A15E03B",
     *     "host_hardware_uuid": "97a65f4e-62ed-479b-9e4e-efa143ac5d5f",
     *     "deleted": false,
     *     "tenant": {
     *         "id": "3805B16C-311C-4ECD-B806-0D205325A23B",
     *         "tenant_name": "Coke",
     *         "config": "{\"name\":\"Coke\",\"deleted\":false,\"plugins\":[{\"name\":\"nova\",\"properties\":[{\"key\":\"endpoint\",\"value\":\"http://www.nova.com\"}]}]}",
     *         "created_by": "admin",
     *         "modified_date": 1468832393433,
     *         "modified_by": "admin",
     *         "deleted": false
     *     }
     * }
     * </pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * // Create the client and call the retrieve API
     * HostAssignments client = new HostAssignments(properties);
     * Response reponse = client.retrieve("3FC59BE5-6352-4530-879C-A4DFDF085BD7");
     * </pre></div>
     */
    public Response retrieve(String id) {
        log.debug("target: {}", getTarget().getUri().toString());
        HashMap<String,Object> map = new HashMap<>();
        map.put("id", id);
        Response response = getTarget().path("host-assignments/{id}").resolveTemplates(map).request(MediaType.APPLICATION_JSON).get(Response.class);
        return response;
    }
    
    /**
     * Deletes tenant to host mapping information.
     * @param id ID of the mapping specified as a path parameter.
     * @return <pre>The serialized Response java model object that was deleted:
     *          id
     *          host_hardware_uuid
     *          deleted
     *          tenant</pre>
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON
     * @mtwMethodType DELETE
     * @mtwSampleRestCall
     * <pre>
     * https://hub.server.com:19445/v1/host-assignments/3FC59BE5-6352-4530-879C-A4DFDF085BD7
     * 
     * Output: {
     *     "id": "7EAAB7AA-C586-42D1-8B79-B72122DB9C7C",
     *     "host_hardware_uuid": "e22aea80-34d4-11e1-a5d8-c03fd56d9c24",
     *     "deleted": true,
     *     "tenant": {
     *         "id": "3805B16C-311C-4ECD-B806-0D205325A23B",
     *         "tenant_name": "Coke",
     *         "config": "{\"name\":\"Coke\",\"deleted\":false,\"plugins\":[{\"name\":\"nova\",\"properties\":
     *                  [{\"key\":\"endpoint\",\"value\":\"http://www.nova.com\"}]}]}",
     *         "created_by": "admin",
     *         "modified_date": 1468832393433,
     *         "modified_by": "admin",
     *         "deleted": false
     *     }
     * }
     * </pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * // Create the client and call the delete API
     * HostAssignments client = new HostAssignments(properties);
     * Response reponse = client.delete("3FC59BE5-6352-4530-879C-A4DFDF085BD7");
     * </pre></div>
     */
    public Response delete(String id) {
        log.debug("target: {}", getTarget().getUri().toString());
        HashMap<String,Object> map = new HashMap<>();
        map.put("id", id);
        Response response = getTarget().path("host-assignments/{id}").resolveTemplates(map).request(MediaType.APPLICATION_JSON).delete(Response.class);
        return response;
    }
    
    /**
     * Searches for tenant to host mappings information.
     * <pre>
     * Returns all the host-assignments if the search string is not provided.
     * </pre>
     * @param criteria The content models of the SearchCriteriaForMapping java model object can be used as query parameters.
     * <pre>
     *          tenantId            ID of tenant.
     *          hostHardwareUuid    Hardware UUID of host.
     *          
     * </pre>
     * @return <pre>The serialized Response java model object that was searched containing list of:
     *          id
     *          host_hardware_uuid
     *          deleted
     *          tenant</pre>
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * <pre>
     * https://hub.server.com:19445/v1/host-assignments?tenant_id=72B99FA9-8FBB-4F20-B988-3990EB4410DA
     * 
     * Output:[
     *     {
     *         "id": "26054082-8C79-4CEF-B1B5-3AA71A15E03B",
     *         "host_hardware_uuid": "97a65f4e-62ed-479b-9e4e-efa143ac5d5f",
     *         "deleted": false,
     *         "tenant": {
     *             "id": "3805B16C-311C-4ECD-B806-0D205325A23B",
     *             "tenant_name": "Coke",
     *             "config": "{\"name\":\"Coke\",\"deleted\":false,\"plugins\":[{\"name\":\"nova\",\"properties\":
     *                      [{\"key\":\"endpoint\",\"value\":\"http://www.nova.com\"}]}]}",
     *             "created_by": "admin",
     *             "modified_date": 1468832393433,
     *             "modified_by": "admin",
     *             "deleted": false
     *         }
     *     },
     *     {
     *         "id": "7EAAB7AA-C586-42D1-8B79-B72122DB9C7C",
     *         "host_hardware_uuid": "e22aea80-34d4-11e1-a5d8-c03fd56d9c24",
     *         "deleted": false,
     *         "tenant": {
     *             "id": "3805B16C-311C-4ECD-B806-0D205325A23B",
     *             "tenant_name": "Coke",
     *             "config": "{\"name\":\"Coke\",\"deleted\":false,\"plugins\":[{\"name\":\"nova\",\"properties\":
     *                      [{\"key\":\"endpoint\",\"value\":\"http://www.nova.com\"}]}]}",
     *             "created_by": "admin",
     *             "modified_date": 1468832393433,
     *             "modified_by": "admin",
     *             "deleted": false
     *         }
     *     }
     * ]                  
     * </pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * // Create the search criteria for mapping model and set the search criteria
     * SearchCriteriaForMapping criteria = new SearchCriteriaForMapping();
     * criteria.tenantId = "72B99FA9-8FBB-4F20-B988-3990EB4410DA";
     * 
     * // Create the client and call the search API
     * HostAssignments client = new HostAssignments(properties);
     * Response reponse = client.search(criteria);
     * </pre></div>
     */
    public Response search(SearchCriteriaForMapping criteria) {
        log.debug("target: {}", getTarget().getUri().toString());
        Response response = getTargetPathWithQueryParams("host-assignments", criteria).request(MediaType.APPLICATION_JSON).get(Response.class);
        return response;
    }
}
