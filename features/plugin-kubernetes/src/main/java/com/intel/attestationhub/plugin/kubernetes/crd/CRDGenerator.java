/**
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes.crd;

import java.util.List;
import com.intel.attestationhub.api.HostDetails;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

//Abstract class for CRD object to be created
public abstract class CRDGenerator {
	protected abstract String createCRD(List<HostDetails> details, String tenantId) throws AttestationHubException;
}
