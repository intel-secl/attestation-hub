/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

/**
 * @author abhishekx.negi@intel.com
 * 
 *         Connector class for HTTP operations
 */
public class KubernetesConnector {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KubernetesConnector.class);

	/**
	 * HTTP GET method
	 * 
	 * In case of any exception occurs during GET operation. Output: { "Logged
	 * error message" : "Error: Get method failed with exception"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Get method failed with
	 *                 exception" }
	 *
	 * @param httpClient
	 *            CloseableHttpClient for HTTP operations
	 * @param uri
	 *            URI to hit
	 * @return HTTP response received
	 */

	protected HttpResponse get(CloseableHttpClient httpClient, URI uri) throws AttestationHubException {
		HttpResponse response = null;
		HttpGet getRequest = new HttpGet(uri);
		try {
			response = httpClient.execute(getRequest);
		} catch (IOException e) {
			log.error("Error: Get method failed with exception ", e);
			throw new AttestationHubException("Error: Get method failed with exception ", e);
		}
		return response;
	}

	/**
	 * HTTP POST method
	 * 
	 * When String cannot be encoded into HttpEntity. Output: { "Logged error
	 * message" : "Error: Unable to encode String into HttpEntity "
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Unable to encode String into
	 *                 HttpEntity " }
	 * 
	 *                 In case of any exception occurs during POST operation.
	 *                 Output: { "Logged error message" : "Error: Post method
	 *                 failed with exception"
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Post method failed with
	 *                 exception" }
	 *
	 * @param httpClient
	 *            CloseableHttpClient for HTTP operations
	 * @param uri
	 *            URI to hit
	 * @param crdObject
	 *            Formatted CRD object input
	 * @return HTTP response received
	 */
	protected HttpResponse post(CloseableHttpClient httpClient, URI uri, String crdObject)
			throws AttestationHubException {
		HttpPost postRequest = new HttpPost(uri);
		HttpEntity entity = null;
		try {
			entity = new StringEntity(crdObject);
		} catch (UnsupportedEncodingException e) {
			log.error("Error: Unable to encode String into HttpEntity ", e);
			throw new AttestationHubException("Error: Unable to encode String into HttpEntity ", e);
		}
		postRequest.setEntity(entity);
		postRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
		HttpResponse response = null;
		try {
			response = httpClient.execute(postRequest);
		} catch (IOException e) {
			log.error("Error: Post method failed with exception ", e);
			throw new AttestationHubException("Error: Post method failed with exception ", e);
		}
		return response;
	}

	/**
	 * HTTP PUT method
	 * 
	 * When String cannot be encoded into HttpEntity. Output: { "Logged error
	 * message" : "Error: Unable to encode String into HttpEntity "
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Unable to encode String into
	 *                 HttpEntity " }
	 * 
	 *                 In case of any exception occurs during POST operation.
	 *                 Output: { "Logged error message" : "Error: Put method
	 *                 failed with exception"
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Put method failed with
	 *                 exception" }
	 *
	 * @param httpClient
	 *            CloseableHttpClient for HTTP operations
	 * @param uri
	 *            URI to hit
	 * @param crdObject
	 *            Formatted CRD object input
	 * @return HTTP response received
	 */
	protected HttpResponse put(CloseableHttpClient httpClient, URI uri, String crdObject)
			throws AttestationHubException {
		HttpPut putRequest = new HttpPut(uri);
		HttpEntity entity = null;
		try {
			entity = new StringEntity(crdObject);
		} catch (UnsupportedEncodingException e) {
			log.error("Error: Unable to encode String into HttpEntity ", e);
			throw new AttestationHubException("Error: Unable to encode String into HttpEntity ", e);
		}
		putRequest.setEntity(entity);
		putRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
		HttpResponse response = null;
		try {
			response = httpClient.execute(putRequest);
		} catch (IOException e) {
			log.error("Error: Put method failed with exception ", e);
			throw new AttestationHubException("Error: Put method failed with exception ", e);
		}
		return response;
	}
}
