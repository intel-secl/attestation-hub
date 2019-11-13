/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.mesos;

import com.intel.attestationhub.api.PublishData;
import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.attestationhub.plugin.EndpointPlugin;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class MesosPluginImpl implements EndpointPlugin {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MesosPluginImpl.class);

    @Override
    public void pushData(PublishData data, Plugin plugin) throws AttestationHubException {

    }

}
