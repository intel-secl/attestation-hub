/**
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes.crd.geolocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonElement;
import com.intel.attestationhub.plugin.kubernetes.crd.template.Spec;

/**
 * @author abhishekx.negi@intel.com
 *
 *         Creates spec field for Geolocation CRD object
 */
public class GeolocationSpec extends Spec {
	private List<LocationSpecFields> hostList = null;

	/**
	 * This method will set the GeolocationSpec fields value and the
	 * GeolocationSpec object in a list.
	 *
	 * @param validTo
	 *            Timestamp till which a host details are valid till.
	 * @param signedTrustReport
	 *            Ecrypted trust report.
	 * @param hostName
	 *            Hostname.
	 * @param assetTags
	 *            Asset tags key,value received.
	 *
	 */
	public void createSpecField(JsonElement validTo, String signedTrustReport, JsonElement hostName,
			Map<String, String> assetTags) {
		LocationSpecFields fields = new LocationSpecFields();
		fields.setValidTo(validTo);
		fields.setHostName(hostName);
		fields.setSignedTrustReport(signedTrustReport);
		fields.setAssetTags(assetTags);
		if (hostList == null) {
			hostList = new ArrayList<>();
		}
		hostList.add(fields);
	}

	/**
	 * Method to get the list of hosts
	 *
	 * @return list of details of hosts
	 */
	public List<LocationSpecFields> getHostList() {
		return hostList;
	}

	protected class LocationSpecFields {
		private JsonElement validTo;
		private String signedTrustReport;
		private JsonElement hostName;
		private Map<String, String> assetTags;

		public Map<String, String> getAssetTags() {
			return assetTags;
		}

		public void setAssetTags(Map<String, String> assetTags) {
			this.assetTags = assetTags;
		}

		public String getSignedTrustReport() {
			return signedTrustReport;
		}

		public void setSignedTrustReport(String signedTrustReport) {
			this.signedTrustReport = signedTrustReport;
		}

		public JsonElement getValidTo() {
			return validTo;
		}

		public void setValidTo(JsonElement validTo) {
			this.validTo = validTo;
		}

		public JsonElement getHostName() {
			return hostName;
		}

		public void setHostName(JsonElement hostName) {
			this.hostName = hostName;
		}

	}
}
