/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.mtwclient;

import java.util.Properties;

//import com.intel.mtwilson.attestation.client.jaxrs.HostAttestations;
import com.intel.mtwilson.flavor.client.jaxrs.Reports;
import com.intel.mtwilson.flavor.client.jaxrs.Hosts;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class MtwClientFactory {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MtwClientFactory.class);

    public static Hosts getHostsClient(Properties properties) throws AttestationHubException {
	Hosts hostsService;
	try {
	    hostsService = new Hosts(properties);
	    log.info("Initialize MTW client for fetching hosts");
	} catch (Exception e) {
	    String errorMsg = "Error creating client for MTW ";
	    log.error(errorMsg, e);
	    throw new AttestationHubException(errorMsg, e);
	}
	return hostsService;
    }

    public static Reports getHostReports(Properties properties) throws AttestationHubException {
	Reports hostReports;
	try {
	    hostReports = new Reports(properties);
	    log.info("Initialize MTW client for fetching host reports");
	} catch (Exception e) {
	    String errorMsg = "Error creating client for MTW ";
	    log.error(errorMsg, e);
	    throw new AttestationHubException(errorMsg, e);
	}
	return hostReports;
    }
}
