/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import com.google.gson.*;
import com.intel.attestationhub.plugin.kubernetes.Constants.Plugin;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

/**
 * @author arijigh
 * 
 *
 */
public class KubernetesClient {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KubernetesClient.class);

	TenantConfig tenantConfig = TenantConfig.getTenantConfigObj();

	protected KubernetesClient() throws AttestationHubException {
		validateUrl(tenantConfig.getPluginApiEndpoint(), Plugin.URL_TYPE);
	}

	/*
	 * It invokes the validateUrl method of ValidationUtil class to validate the
	 * pluginApiEndpoint configuration parameter received from Attestation hub.
	 * 
	 */
	private void validateUrl(String pluginApiEndpoint, String type) throws AttestationHubException {

		ValidationUtil.validateUrl(pluginApiEndpoint, type);

	}

	/**
	 * This method provides the input to build endpoint URL after determining
	 * the type of formatted CRD object received
	 *
	 * @param jsonList
	 *            IP address of the Kubernetes Master machine, including the
	 *            port number on which of Kubernetes cluster is running.
	 * 
	 */
	public void sendDataToEndpoint(List<String> jsonList) throws AttestationHubException {
		for (String json : jsonList) {
			if (!json.equals(Plugin.NULL)) {
				JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
				String tenantId = jsonObject.getAsJsonObject(Plugin.METADATA).get(Plugin.NAME).toString()
						.replace(Plugin.SLASH_COMMA, Plugin.EMPTY_STRING);
				String urlKind = Plugin.URL_HOSTATTRIBUTES;
				// Create an if block for new CRD
				// To build an URI invoke buildEndpointUri method of this class
				URI uri = buildEndpointUri(jsonObject, tenantId, urlKind);
				new KubernetesCRDUtil().publishCrdToK8s(uri, json);
			}
		}
	}

	/*
	 * Builds the endpoint URI according to the type of CRD input received.
	 *
	 * When some issue occurs while building the URI. Output: { "Logged error
	 * message" : "Error: Failed building endpoint URI <uri-val>"
	 *
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Failed building endpoint URI
	 *                 <uri-val>" }
	 * @param json
	 *            JsonObject of the CRD type
	 * @param tenantId
	 *            Tenant Id for which the CRD object to be created
	 * @param kind
	 *            Type of CRD for which the endpoint URI has to be build
	 *
	 * @return URI after building, that is, this URI will be called for CRD
	 *         object operations
	 */
	private URI buildEndpointUri(JsonObject json, String tenantId, String kind) throws AttestationHubException {
		String urlString = tenantConfig.getPluginApiEndpoint() + Plugin.PATH + kind + Plugin.SLASH + tenantId;
		URI uri;
		try {
			uri = new URI(urlString);
		} catch (URISyntaxException e) {
			log.error("Error: Failed building endpoint URI", e);
			throw new AttestationHubException("Error: Failed building endpoint URI", e);
		}
		return uri;
	}

}
