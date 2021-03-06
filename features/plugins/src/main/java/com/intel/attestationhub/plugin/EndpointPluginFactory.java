/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

/**
 * 
 */
package com.intel.attestationhub.plugin;

import org.apache.commons.lang.StringUtils;

import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.dcsg.cpg.extensions.Plugins;

/**
 * @author Vijay Prakash
 *
 */
public class EndpointPluginFactory {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EndpointPluginFactory.class);

    public static EndpointPlugin getPluginImpl(Plugin plugin) {

	String providerClass = plugin.extractProviderClass();
	if (StringUtils.isBlank(providerClass)) {
	    log.error("No provider configured for plugin");
	    return null;
	}
	EndpointPlugin endpointPlugin = null;
	Class delegateClass = null;
	try {
		delegateClass = Class.forName(providerClass);
		endpointPlugin = (EndpointPlugin) delegateClass.newInstance();
	} catch (ReflectiveOperationException e) {
		log.error("Plugin: {} not found due to: {}", providerClass, e.getMessage());
		return null;
	}
	return endpointPlugin;
    }
}
