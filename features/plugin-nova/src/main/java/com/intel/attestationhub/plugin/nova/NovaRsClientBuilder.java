/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.nova;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.attestationhub.api.Tenant.PluginProperty;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class NovaRsClientBuilder {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NovaRsClientBuilder.class);

    public static NovaRsClient build(Plugin plugin) throws AttestationHubException {
        if (plugin == null) {
            throw new AttestationHubException("No configuration provided ");
        }
        List<PluginProperty> properties = plugin.getProperties();
        String pluginAuthEndpoint = null, pluginAuthVersion = null, userName = null, password = null, tenantName = null,
                domainName = null;
        for (PluginProperty property : properties) {
            switch (property.getKey()) {
            case Constants.AUTH_ENDPOINT:
                pluginAuthEndpoint = property.getValue();
                break;
            case Constants.DOMAIN_NAME:
                domainName = property.getValue();
                break;
            case Constants.KEYSTONE_VERSION:
                pluginAuthVersion = property.getValue();
                break;
            case Constants.PASSWORD:
                password = property.getValue();
                break;
            case Constants.USERNAME:
                userName = property.getValue();
                break;
            case Constants.TENANT_NAME:
                tenantName = property.getValue();
                break;
            }
        }

        if (StringUtils.isBlank(pluginAuthEndpoint) || StringUtils.isBlank(pluginAuthVersion)
                || StringUtils.isBlank(password) || StringUtils.isBlank(userName)) {
            log.error(
                    "Configuration not provided : pluginAuthEndpoint: {}, pluginAuthVersion : {}, password: {}, userName: {}",
                    pluginAuthEndpoint, pluginAuthVersion, password, userName);
            throw new AttestationHubException("Please provide mandatory configuration for authorization");
        }

        return new NovaRsClient(pluginAuthEndpoint, tenantName, userName, password, domainName, pluginAuthVersion);
    }

}
