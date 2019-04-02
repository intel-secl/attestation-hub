/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin;

import com.intel.attestationhub.api.PublishData;
import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public interface EndpointPlugin {
    public void pushData(PublishData data, Plugin plugin) throws AttestationHubException;
}
