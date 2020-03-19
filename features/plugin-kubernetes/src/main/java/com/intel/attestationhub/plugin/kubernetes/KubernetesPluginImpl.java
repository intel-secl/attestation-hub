/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes;

import java.util.List;

import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.intel.attestationhub.api.HostDetails;
import com.intel.attestationhub.api.PublishData;
import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.attestationhub.plugin.EndpointPlugin;
import com.intel.attestationhub.plugin.kubernetes.crd.CRDManager;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

/**
 * @author abhishekx.negi@intel.com
 *
 *         Starting point of the Kubernetes Plugin. We have defined this class
 *         to be initialized when any data regarding Kubernetes tenant is pushed
 *         from Attestation Hub.
 */
public class KubernetesPluginImpl implements EndpointPlugin {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KubernetesPluginImpl.class);

	/**
	 * This method overrides the method of EndpointPlugin. During publish of
	 * data Attestation Hub will invoke this method and in turn it will validate
	 * the data received.
	 *
	 * When received data, that is, object of PublishData class cannot be
	 * converted into JSON. Output: { "Logged error message" : "Error: Failed
	 * converting data into JSON"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Failed converting data into
	 *                 JSON" } When converted data is empty. Output: { "Logged
	 *                 error message" : "Error: No data to publish" }
	 * 
	 * @param data
	 *            It will contain the tenant data pushed from Attestation Hub.
	 * @param plugin
	 *            It contains the tenant configuration, which was provided
	 *            during registration of tenant.
	 * 
	 */
	@Override
	public void pushData(PublishData data, Plugin plugin) throws AttestationHubException {
		String jsonData;
		try {
			log.info("Info: Begin publishing kubernetes plugin data");
			jsonData = new Gson().toJson(data);
			log.info("Info: Data from attestation hub " + jsonData);
		} catch (Exception e) {
			log.error("Error: Failed converting data to JSON");
			throw new AttestationHubException("Error: Failed converting data to JSON", e);
		}
		if (StringUtils.isBlank(jsonData)) {
			log.error("Error: No data to publish");
			return;
		}
		validatePublishData(data);
		KubernetesClient kubernetesClient = new KubernetesConfig().build(plugin);
		if (TenantConfig.getTenantConfigObj().isVmWorkerDisabled()) {
			kubernetesClient.sendDataToEndpoint(new CRDManager().generateCrd(data));
		} else {
			List<String> bmCrdObjList=new CRDManager().generateCrd(data);
			kubernetesClient.sendDataToEndpoint(new KubernetesClient().buildVMData(bmCrdObjList,data.hostDetailsList,
					kubernetesClient.getWorkerNodeDetails()));
		}
	}

	/**
	 * This method validates the tenant data received from Attestation Hub.
	 *
	 * When empty host details for a tenant received from Attestation Hub.
	 * Output: { "Logged error message" : "Error: Empty host details from
	 * Attestation Hub"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Empty host details from
	 *                 Attestation Hub" } When there is no tenant Id present in
	 *                 the received data. Output: { "Logged error message" :
	 *                 "Error: Empty TenantId field in host's detail"
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Empty TenantId field in host's
	 *                 detail" }
	 * 
	 * @param publishData
	 *            It will contain the tenant data pushed from Attestation Hub.
	 * 
	 */
	private void validatePublishData(PublishData publishData) throws AttestationHubException {
		List<HostDetails> details = publishData.hostDetailsList;
		if (details.size() == Constants.Plugin.ZERO) {
			log.error("Error: Empty host details from Attestation Hub");
			throw new AttestationHubException("Error: Empty host details from Attestation Hub");
		}
		String tenantId = publishData.tenantId;
		if (tenantId.isEmpty()) {
			log.error("Error: Empty TenantId field in host's detail");
			throw new AttestationHubException("Error: Empty TenantId field in host's detail");
		}
	}
}
