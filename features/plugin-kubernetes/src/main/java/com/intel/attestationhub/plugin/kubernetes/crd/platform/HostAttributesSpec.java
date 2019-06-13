/**
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes.crd.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.intel.attestationhub.plugin.kubernetes.crd.template.Spec;

/**
 * @author abhishekx.negi@intel.com
 *
 *         Creates spec field for HostAttributes CRD object
 */
public class HostAttributesSpec extends Spec {
	private List<HostAttributesSpecFields> hostList = new ArrayList<>();

	/**
	 * This method will set the HostAttributesSpec fields value and the
	 * HostAttributesSpec object in a list.
	 *
	 * @param hostName
	 *            Hostname.
	 * @param trusted
	 *            Boolean value which determines a host is trusted or not.
	 * @param validTo
	 *            Timestamp till which a host details are valid till.
	 * @param signedTrustReport
	 *            Ecrypted trust report.
	 * @param assetTags
	 *            Asset tags key, value received.
	 *
	 */
	public void createSpecField(JsonElement hostName, JsonElement trusted, JsonElement validTo,
								String signedTrustReport, Map<String, String> assetTags) {
		HostAttributesSpecFields fields = new HostAttributesSpecFields();
		fields.setValidTo(validTo);
		fields.setHostName(hostName);
		fields.setSignedTrustReport(signedTrustReport);
		if(assetTags != null) {
			fields.setAssetTags(assetTags);
		}
		fields.setTrusted(trusted);
		hostList.add(fields);
	}

	/**
	 * Method to get the list of hosts
	 *
	 * @return list of details of hosts
	 */
	public List<HostAttributesSpecFields> getHostList() {
		return hostList;
	}

	protected class HostAttributesSpecFields {

		private JsonElement hostName;
		private JsonElement validTo;
		private String signedTrustReport;
		private JsonElement trusted;
		private Map<String, String> assetTags;

		public Map<String, String> getAssetTags() {
			return assetTags;
		}
		public void setAssetTags(Map<String, String> assetTags) {
			this.assetTags = assetTags;
		}

		public JsonElement getHostName() {
			return hostName;
		}

		public void setHostName(JsonElement hostName) {
			this.hostName = hostName;
		}

		public JsonElement getValidTo() {
			return validTo;
		}

		public void setValidTo(JsonElement validTo) {
			this.validTo = validTo;
		}

		public JsonElement getTrusted() {
			return trusted;
		}

		public void setTrusted(JsonElement trusted) {
			this.trusted = trusted;
		}

		public String getSignedTrustReport() {
			return signedTrustReport;
		}

		public void setSignedTrustReport(String signedTrustReport) {
			this.signedTrustReport = signedTrustReport;
		}

	}

}
