/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.identity;

public class IdentityServiceFactory {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IdentityServiceFactory.class);

    public static IdentityService getIdentityService(String versionType) {
	IdentityService identityService = null;
	log.info("Finding identity service");
	if (versionType.equalsIgnoreCase(IdentityService.VERSION_V2)) {
	    identityService = new V2IdentityServiceImpl();
	    log.debug("V2 identity service");
	} else if (versionType.equalsIgnoreCase(IdentityService.VERSION_V3)) {
	    identityService = new V3IdentityServiceImpl();
	    log.debug("V3 identity service");
	} else {
        log.error("Unknown Version type: {}", versionType);
    }

	return identityService;

    }

}
