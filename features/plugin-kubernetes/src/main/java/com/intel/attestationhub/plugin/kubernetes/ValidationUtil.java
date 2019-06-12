/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.lang.StringUtils;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

/**
 * @author abhishekx.negi@intel.com
 *	
 * Validation class
 */
public class ValidationUtil {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ValidationUtil.class);

	/**
	 * Contains all the pluginApiEndpoint(URL) validation
	 *
	 *		In case of malformed URL
	 * 		Output: 
	 * 		{ 
	 * 		"Logged error message" : "Error: Invalid API url"
	 * 		@exception: AttestationHubException with the message, "Error: Invalid API url"
	 *      }
	 * 
	 * 		When no host is specified in URL
	 * 		Output: 
	 * 		{ 
	 * 		"Logged error message" : "Error: No host specified"
	 * 		@exception: AttestationHubException with the message, "Error: No host specified"
	 *      }
	 * 		When no protocol is specified in URL
	 * 		Output: 
	 * 		{ 
	 * 		"Logged error message" : "Error: No protocol specified"
	 * 		@exception: AttestationHubException with the message, "Error: No protocol specified"
	 *      }
	 *      
	 * 		When no port is specified in URL
	 * 		Output: 
	 * 		{ 
	 * 		"Logged error message" : "Error: No port specified"
	 * 		@exception: AttestationHubException with the message, "Error: No port specified"
	 *      }
	 *      
	 * 		When some path is defined in URL
	 * 		Output: 
	 * 		{ 
	 * 		"Logged error message" : "Error: Invalid endpoint format- accepted format, http(s)://HOST:PORT"
	 * 		@exception: AttestationHubException with the message, "Error: Invalid endpoint format- accepted format, http(s)://HOST:PORT"
	 *      }
	 *      
	 * 
	 * @param urlStr
	 *            IP address of the Kubernetes Master machine, including the port number on which of Kubernetes cluster is running.
	 * @param type
	 *            Type of URL
	 * 
	 */
	protected static void validateUrl(String urlStr, String type) throws AttestationHubException {
		log.info("Info: Validating plugin's Endpoint URL");
		if (StringUtils.isBlank(urlStr)) {
			throw new AttestationHubException("Error: Invalid " + type + " url");
		}
		URL url;
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException e) {
			log.error("Error: Invalid " + type + " url");
			throw new AttestationHubException("Error: Invalid " + type + " url");
		}
		if (StringUtils.isBlank(url.getHost())) {
			log.error("Error: No host specified");
			throw new AttestationHubException("Error: No host specified");
		}
		if (StringUtils.isBlank(url.getProtocol())) {
			log.error("Error: No protocol specified");
			throw new AttestationHubException("Error: No protocol specified");
		}
		if (url.getPort() == -1) {
			log.error("Error: No port specified");
			throw new AttestationHubException("Error: No port specified");
		}
		if (StringUtils.isNotBlank(url.getPath())) {
			log.error("Error: Invalid endpoint format- accepted format, http(s)://HOST:PORT");
			throw new AttestationHubException("Error: Invalid endpoint format- accepted format, http(s)://HOST:PORT");
		}
	}
}
