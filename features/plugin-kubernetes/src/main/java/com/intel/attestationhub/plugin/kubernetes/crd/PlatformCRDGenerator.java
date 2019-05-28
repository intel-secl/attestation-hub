/**
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes.crd;

import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intel.attestationhub.api.HostDetails;
import com.intel.attestationhub.plugin.kubernetes.crd.platform.PlatformCRD;
import com.intel.attestationhub.plugin.kubernetes.crd.platform.PlatformMetadata;
import com.intel.attestationhub.plugin.kubernetes.crd.platform.PlatformSpec;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

/**
 * @author abhishekx.negi@intel.com
 *
 *         Creates the input for creating a Platform CRD object.
 */
public class PlatformCRDGenerator extends CRDGenerator {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PlatformCRDGenerator.class);

	/**
	 * It parses the mapped host details of a tenant and fetches the variables
	 * required for Platform CRD object. After fetching the required variables
	 * it put them in the valid CRD object format required to create a Platform
	 * CRD object.
	 *
	 * When any required CRD parameters are missing in host details data.
	 * Output: { "Logged error message" : "Error: Empty trust_report fields-
	 * valid_to or hostname or signed_trust_report"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Empty trust_report fields-
	 *                 valid_to or hostname or signed_trust_report" } When
	 *                 trusted field is not there in the host's signed report.
	 *                 Output: { "Logged error message" : "Error: Trusted field
	 *                 does not exists"
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Trusted field does not exists"
	 *                 }
	 * 
	 * @param details
	 *            Host details mapped to the tenant
	 * @param tenantId
	 *            Tenant Id for which the data is received from the Attestation
	 *            Hub
	 * @return a formatted input for creating a Platform CRD object.
	 * 
	 */
	@Override
	protected String createCRD(List<HostDetails> details, String tenantId) throws AttestationHubException {
		PlatformSpec platformSpec = new PlatformSpec();
		for (HostDetails hostDetails : details) {
			JsonObject jsonObject = new JsonParser().parse(hostDetails.trust_report).getAsJsonObject();
			JsonElement validTo = jsonObject.get(Constants.VALID_TO);
			JsonElement hostName = jsonObject.get(Constants.HOSTNAME);
			String signedReport = hostDetails.signed_trust_report;
			if (validTo == null || hostName == null || signedReport.isEmpty()) {
				log.error("Error: Empty trust_report fields- valid_to or hostname or signed_trust_report");
				throw new AttestationHubException(
						"Error: Empty trust_report fields- valid_to or hostname or signed_trust_report");
			}
			JsonElement trusted = jsonObject.get(Constants.TRUSTED);
			if (trusted == null) {
				log.error("Error: Trusted field does not exists");
				throw new AttestationHubException("Error: Trusted field does not exists");
			}
			platformSpec.createSpecField(hostName, trusted, validTo, signedReport);
		}
		PlatformCRD crd = null;
		if (platformSpec.getHostList() != null) {
			PlatformMetadata metadata = new PlatformMetadata();
			metadata.setName(tenantId.toLowerCase() + Constants.HYPHEN + Constants.CIT + Constants.HYPHEN
					+ Constants.PLATFORM + Constants.HYPHEN + Constants.OBJECT);
			crd = new PlatformCRD();
			crd.setApiVersion(Constants.API_VERSION);
			crd.setKind(Constants.PLATFORM_CRD);
			crd.setMetadata(metadata);
			crd.setSpec(platformSpec);
		}
		return new Gson().toJson(crd);
	}
}
