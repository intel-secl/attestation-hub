/**
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes.crd;

import java.util.ArrayList;
import java.util.List;
import com.intel.attestationhub.api.PublishData;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

/**
 * @author abhishekx.negi@intel.com
 *
 *         Decides which CRD object will be created.
 */
public class CRDManager {

	/**
	 * This method invokes the CRD generator class of the CRD object's to be
	 * created by verifying the boolean value from the Constants file. It return
	 * an input for creating CRD object(s).
	 *
	 * @param data
	 *            The tenant data received from Attestation Hub.
	 * @return list of CRD object data to be created in a CRD object format.
	 * 
	 */
	public List<String> generateCrd(PublishData data) throws AttestationHubException {
		List<String> jsonList = new ArrayList<>();
		if (Constants.CREATE_PLATFORM_CRD) {
			jsonList.add(new PlatformCRDGenerator().createCRD(data.hostDetailsList, data.tenantId));
		}
		if (Constants.CREATE_GEOLOCATION_CRD) {
			jsonList.add(new GeolocationCRDGenerator().createCRD(data.hostDetailsList, data.tenantId));
		}
		// Make an entry here for new CRD object to be created. See upper if
		// statements
		// for reference.
		return jsonList;
	}
}
