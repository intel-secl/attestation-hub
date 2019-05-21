/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.attestationhub.client.jaxrs2;

import com.intel.mtwilson.jaxrs2.client.MtWilsonClient;
import com.intel.attestationhub.api.HostFilterCriteria;
import java.util.HashMap;
import java.util.Properties;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * These resources are used to retrieve hosts of Host Verification Service.
 * <pre>
 * The Hosts API is used to retrieve the current list of hosts seen by the 
 * Integration Hub. The Integration Hub regularly polls the Host Verification 
 * Service for all new Attestation Reports, and uses those Reports to update the 
 * Integration Hub’s internal database of hosts. Unlike the Verification Service, 
 * the Hub does not retain old Host Status or Report information. The Hub will 
 * retain only the most recent currently valid Attestation Report for each host.
 * </pre>
 */
public class Hosts extends MtWilsonClient {

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
     * Hosts client = new Hosts(properties);
     * </pre>
     * @throws Exception 
     */
    public Hosts(Properties properties) throws Exception {
        super(properties);
    }
    
    /**
     * Retrieves a host.
     * <pre>
     * If ID is not in proper format, it throws error 602, Invalid ID.
     * If there is failure on server side, it throws 601, Request processing failed.
     * </pre>
     * @param id ID of the host specified as a path parameter.
     * @return <pre>The serialized Response java model object that was retrieved:
     *          id
     *          hardware_uuid
     *          host_name
     *          bios_mle_uuid
     *          aik_sha1
     *          connection_url
     *          saml_report
     *          modified_date
     *          deleted</pre>
     * @since ISecL 1.0
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * https://hub.server.com:19445/v1/hosts/97a65f4e-62ed-479b-9e4e-efa143ac5d5e 
     *              
     * Output: {
     *      "id": "63ab5eee-2888-457e-8e87-3e07402c2449",
     *      "hardware_uuid": "80e88d91-69d6-e711-906e-001560a04062",
     *      "host_name": "RHEL_HOST",
     *      "connection_url": "https://trustagent.server.com:1443",
     *      "trust_tags_json": "{"hostname":"192.168.0.1",
     *                          "trust":{"TRUST_OVERALL":"false","TRUST_ASSET_TAG":"NA","TRUST_HOST_UNIQUE":"NA","TRUST_BIOS":"false",
     *                          "TRUST_OS":"false"}}",
     *      "valid_to": "2018-06-08T17:11:15.079Z",
     *      "saml_report": "<?xml version="1.0" encoding="UTF-8"?> <saml2:Assertion ID="MapAssertion" 
     *                      IssueInstant="2018-06-08T16:11:15.078Z" Version="2.0" xmlns:saml2="256"/>nveloped-signature"/>...</saml2:Assertion>",
     *      "created_date": "2018-06-07T13:37:19-0700",
     *      "created_by": "admin",
     *      "modified_date": "2018-06-08T09:11:50-0700",
     *      "modified_by": "admin",
     *      "deleted": false,
     *      "trusted": false,
     *      "asset_tags": "{}"
     * }
     * </div></pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * // Create the client and call the retrieve API
     * Hosts client = new Hosts(properties);
     * Response reponse = client.retrieve("97a65f4e-62ed-479b-9e4e-efa143ac5d5e ");
     * </pre></div>
     */
    public Response retrieve(String id) {
        log.debug("target: {}", getTarget().getUri().toString());
        HashMap<String,Object> map = new HashMap<>();
        map.put("id", id);
        Response response = getTarget().path("hosts/{id}").resolveTemplates(map).request(MediaType.APPLICATION_JSON).get(Response.class);
        return response;
    }
    
    /**
     * Searches for hosts.
     * <pre>
     * Returns all hosts information if the search string is not provided.
     * If ID is not in proper format, it throws error 602, Invalid ID.
     * If there is failure on server side, it throws 601, Request processing failed.
     * </pre>
     * @param filterCriteria The content models of the HostFilterCriteria java model object can be used as query parameters.
     * <pre>
     *          nameEqualTo        Name of the hosts.
     * </pre>
     * @return <pre>The serialized Response java model object that was searched:
     *          id
     *          hardware_uuid
     *          host_name
     *          bios_mle_uuid
     *          aik_sha1
     *          connection_url
     *          saml_report
     *          modified_date
     *          deleted</pre>
     * @mtwRequiresPermissions None
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * https://hub.server.com:19445/v1/hosts?nameEqualTo=RHEL_Host
     *                    
     * Output: {
     *      "id": "63ab5eee-2888-457e-8e87-3e07402c2449",
     *      "hardware_uuid": "80e88d91-69d6-e711-906e-001560a04062",
     *      "host_name": "RHEL_HOST",
     *      "connection_url": "https://trustagent.server.com:1443",
     *      "trust_tags_json": "{"hostname":"192.168.0.1",
     *                          "trust":{"TRUST_OVERALL":"false","TRUST_ASSET_TAG":"NA","TRUST_HOST_UNIQUE":"NA","TRUST_BIOS":"false",
     *                          "TRUST_OS":"false"}}",
     *      "valid_to": "2018-06-08T17:11:15.079Z",
     *      "saml_report": "<?xml version="1.0" encoding="UTF-8"?> <saml2:Assertion ID="MapAssertion" 
     *                      IssueInstant="2018-06-08T16:11:15.078Z" Version="2.0" xmlns:saml2="256"/>nveloped-signature"/>...</saml2:Assertion>",
     *      "created_date": "2018-06-07T13:37:19-0700",
     *      "created_by": "admin",
     *      "modified_date": "2018-06-08T09:11:50-0700",
     *      "modified_by": "admin",
     *      "deleted": false,
     *      "trusted": false,
     *      "asset_tags": "{}"
     * }
     * </div></pre>
     * @mtwSampleApiCall
     * <div style="word-wrap: break-word; width: 1024px"><pre>
     * // Create the host filter criteria model and set the search criteria
     * HostFilterCriteria filterCriteria = new HostFilterCriteria();
     * filterCriteria.nameEqualTo = "RHEL_Host";
     * 
     * // Create the client and call the search API
     * Hosts client = new Hosts(properties);
     * Response reponse = client.search(filterCriteria);
     * </pre></div>
     */
    public Response search(HostFilterCriteria filterCriteria) {
        log.debug("target: {}", getTarget().getUri().toString());
        Response response = getTargetPathWithQueryParams("hosts", filterCriteria).request(MediaType.APPLICATION_JSON).get(Response.class);
        return response;
    }
}
