/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.kubernetes;

import java.io.IOException;
import java.net.URI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intel.attestationhub.plugin.kubernetes.Constants.Plugin;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

/**
 * @author abhishekx.negi@intel.com
 * 
 *         This class performs the CRD object operations
 */
public class KubernetesCRDUtil {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KubernetesCRDUtil.class);

	/**
	 * Manages the HTTP operations for CRD objects, that is, if the CRD object
	 * already exists(200) then a PUT call will be performed else POST. The HTTP
	 * operations will be performed only after get the HttpClient from
	 * KubernetesCertificateAuthenticator class
	 * 
	 * 
	 * When CRD does not exists. Output: { "Logged error message" : "Error:
	 * <crd-uri> CRD does not exists"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: <crd-uri> CRD does not exists"
	 *                 }
	 * 
	 *
	 * @param uri
	 *            URI to be hit
	 * @param payload
	 *            Formatted input of the CRD object
	 * @param clientKeystore
	 *            Client keystore
	 * @param clientKeystorePass
	 *            Client keystore pass
	 * @param serverKeystore
	 *            Server keystore
	 * @param serverKeystorePass
	 *            Server keystore pass
	 */
	protected void publishCrdToK8s(URI uri, String payload) throws AttestationHubException {
		KubernetesConnector connector = new KubernetesConnector();
		CloseableHttpClient httpClient = new KubernetesCertificateAuthenticator().getHttpClient();
		HttpResponse response = getCrd(uri, connector, httpClient);
		HttpEntity entity = response.getEntity();
		JsonObject jsonObject = null;
		try {
			// CRD is required to create a CRD object. Exception will occur if CRD doesn't exists.
			jsonObject = new JsonParser().parse(EntityUtils.toString(entity)).getAsJsonObject();
		} catch (JsonSyntaxException | IOException e) {
			log.error("Error: " + uri + " CRD does not exists");
			throw new AttestationHubException("Error: " + uri + " CRD does not exists");
		}
		// If CRD object already exists for a tenant then updating it else creating a new one
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			jsonObject = jsonObject.getAsJsonObject(Plugin.METADATA);
			// Resource version is generated on K8S side after we create a CRD object. 
			// It is required for PUT operation.
			String resourceVersion = jsonObject.get(Plugin.RESOURCE_VERSION).toString().replace(Plugin.SLASH_COMMA,
					Plugin.EMPTY_STRING);
			JsonObject payloadJson = new JsonParser().parse(payload).getAsJsonObject();
			payloadJson.getAsJsonObject(Plugin.METADATA).addProperty(Plugin.RESOURCE_VERSION, resourceVersion);
			putCrd(uri, payloadJson.toString(), connector, httpClient);
		} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
			postCrd(uri, payload, connector, httpClient);
		}

	}

	/**
	 * Performs the HTTP PUT operation
	 *
	 * When the response recieved other than 200 Output: { "Logged error
	 * message" : "Error: Put CRD failed with code <response-code>"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Put CRD failed with code
	 *                 <response-code>" }
	 * 
	 *
	 * @param uri
	 *            URI to be hit
	 * @param payload
	 *            Formatted input of the CRD object
	 * @param connector
	 *            KubernetesConnector class object
	 * @param httpClient
	 *            CloseableHttpClient for HTTP operations
	 */
	private void putCrd(URI uri, String payload, KubernetesConnector connector, CloseableHttpClient httpClient)
			throws AttestationHubException {
		HttpResponse httpResponse = connector.put(httpClient, uri, payload);
		if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			log.error("Error: Put CRD failed with code", httpResponse.getStatusLine());
			throw new AttestationHubException("Error: Put CRD failed with code" + httpResponse.getStatusLine());
		}
	}

	/**
	 * Performs the HTTP POST operation
	 *
	 * When the response recieved other than 201 Output: { "Logged error
	 * message" : "Error: Post CRD failed with code <response-code>"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Post CRD failed with code
	 *                 <response-code>" }
	 * 
	 *
	 * @param uri
	 *            URI to be hit
	 * @param payload
	 *            Formatted input of the CRD object
	 * @param connector
	 *            KubernetesConnector class object
	 * @param httpClient
	 *            CloseableHttpClient for HTTP operations
	 */
	private void postCrd(URI uri, String payload, KubernetesConnector connector, CloseableHttpClient httpClient)
			throws AttestationHubException {
		HttpResponse httpResponse = connector.post(httpClient, uri, payload);
		if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
			log.error("Error: Post CRD failed with code", httpResponse.getStatusLine());
			throw new AttestationHubException("Error: Post CRD failed with code" + httpResponse.getStatusLine());
		}
	}

	/**
	 * Performs the HTTP GET operation
	 * 
	 * @param uri
	 *            URI to be hit
	 * @param payload
	 *            Formatted input of the CRD object
	 * @param connector
	 *            KubernetesConnector class object
	 * @param httpClient
	 *            CloseableHttpClient for HTTP operations
	 * @return HTTP response received
	 */
	private HttpResponse getCrd(URI uri, KubernetesConnector connector, CloseableHttpClient httpClient)
			throws AttestationHubException {
		return new KubernetesConnector().get(httpClient, uri);
	}
}
