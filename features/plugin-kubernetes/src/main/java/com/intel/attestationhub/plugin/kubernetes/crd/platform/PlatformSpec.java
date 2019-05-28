/**
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes.crd.platform;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonElement;
import com.intel.attestationhub.plugin.kubernetes.crd.template.Spec;

/**
 * @author abhishekx.negi@intel.com
 *
 *         Creates spec field for Platform CRD object
 */
public class PlatformSpec extends Spec {
	private List<PlatformSpecFields> hostList = null;

	/**
	 * This method will set the PlatformSpec fields value and the PlatformSpec
	 * object in a list.
	 *
	 * @param hostName
	 *            Hostname.
	 * @param trusted
	 *            Boolean value which determines a host is trusted or not.
	 * @param validTo
	 *            Timestamp till which a host details are valid till.
	 * @param signedTrustReport
	 *            Ecrypted trust report.
	 */
	public void createSpecField(JsonElement hostName, JsonElement trusted, JsonElement validTo,
			String signedTrustReport) {
		PlatformSpecFields fields = new PlatformSpecFields();
		fields.setHostName(hostName);
		fields.setTrusted(trusted);
		fields.setValidTo(validTo);
		fields.setSignedTrustReport(signedTrustReport);
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
	public List<PlatformSpecFields> getHostList() {
		return hostList;
	}

	protected class PlatformSpecFields {

		private JsonElement hostName;
		private JsonElement validTo;
		private String signedTrustReport;
		private JsonElement trusted;

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
