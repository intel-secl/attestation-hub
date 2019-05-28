/**
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes.crd.geolocation;

import com.intel.attestationhub.plugin.kubernetes.crd.template.KubernetesCRD;
import com.intel.attestationhub.plugin.kubernetes.crd.template.Metadata;
import com.intel.attestationhub.plugin.kubernetes.crd.template.Spec;

/**
 * @author abhishekx.negi@intel.com
 *
 *         Base class of Geolocation CRD object
 */
public class GeolocationCRD extends KubernetesCRD {

	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public void setSpec(Spec spec) {
		this.spec = spec;
	}

}
