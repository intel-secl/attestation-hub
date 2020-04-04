/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.attestationhub.api.Tenant.PluginProperty;
import com.intel.attestationhub.plugin.kubernetes.Constants.Tenant;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

/**
 * @author abhishekx.negi@intel.com
 *
 *         Validates the tenant configuration received from Attestation Hub.
 */
public class KubernetesConfig {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KubernetesConfig.class);
	TenantConfig tenantConfig = TenantConfig.getTenantConfigObj();

	/**
	 * It validates the tenant configuration parameters- pluginApiEndpoint,
	 * tenantName, clientKeystore, clientKeystorePass, serverKeystore and
	 * serverKeystorepass.
	 *
	 * When no plugin configuration is provided Output: {
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: No configuration provided" }
	 * 
	 *                 When any configuration parameter is missing or is empty
	 *                 Output: { "Logged error message" : "Error: Invalid tenant
	 *                 configuration"
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Invalid tenant configuration" }
	 * 
	 *                 If any exception occurs while reading the tenant
	 *                 configuration Output: { "Logged error message" : "Error:
	 *                 Invalid plugin endpoints"
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Invalid plugin endpoints" }
	 * 
	 * @param plugin
	 *            It contains the tenant configuration, which was provided
	 *            during registration a tenant.
	 * @return an initialized object of KubernetesClient class.
	 * 
	 */
	protected KubernetesClient build(Plugin plugin) throws AttestationHubException {
		try {
			if (plugin == null) {
				throw new AttestationHubException("No configuration provided");
			}

			List<PluginProperty> properties = plugin.getProperties();
			for (PluginProperty PluginProperty : properties) {
				switch (PluginProperty.getKey()) {
                    			case Tenant.API_ENDPOINT:
		        	                tenantConfig.setPluginApiEndpoint(PluginProperty.getValue());
               				        break;
			                case Tenant.TENANT_NAME:
                        			tenantConfig.setTenantName(PluginProperty.getValue());
                        			break;
		                        case Tenant.VM_WORKER_DISABLED:
                    				tenantConfig.setVmWorkerDisabled(Boolean.parseBoolean(PluginProperty.getValue()));
			                        break;
			                case Tenant.KEYSTONE_VERSION:
                        			tenantConfig.setKeystoneVersion(PluginProperty.getValue());
			                        break;
			                case Tenant.OPENSTACK_TENANT_NAME:
                        			tenantConfig.setOpenstackTenantName(PluginProperty.getValue());
			                        break;
			                case Tenant.OPENSTACK_SCOPE:
			                        tenantConfig.setOpenstackScope(PluginProperty.getValue());
			                        break;
			                case Tenant.OPENSTACK_USERNAME:
			                        tenantConfig.setOpenstackUsername(PluginProperty.getValue());
                        			break;
			                case Tenant.OPENSTACK_PASS:
			                        tenantConfig.setOpenstackPass(PluginProperty.getValue());
			                        break;
			                case Tenant.OPENSTACK_URI:
                			        tenantConfig.setOpenstackURI(PluginProperty.getValue());
			                        break;
					         case Tenant.KUBERNETES_API_BEARER_TOKEN:
									tenantConfig.setBearerToken(PluginProperty.getValue());
									break;
			                case Tenant.KUBERNETES_API_SERVER_KEYSTORE:
                        			tenantConfig.setServerKeystore(PluginProperty.getValue());
			                        break;
			                case Tenant.KUBERNETES_API_SERVER_KEYSTORE_PASSWORD:
			                        tenantConfig.setServerKeystorePass(PluginProperty.getValue());
						break;
				}

			}
			if (StringUtils.isBlank(tenantConfig.getTenantName())
					|| StringUtils.isBlank(tenantConfig.getPluginApiEndpoint())
					|| StringUtils.isBlank(tenantConfig.getBearerToken())
			                || StringUtils.isBlank(tenantConfig.getServerKeystore())
		                        || StringUtils.isBlank(tenantConfig.getServerKeystorePass())
	                ) {
				log.error("Error: Invalid tenant configuration");
				throw new AttestationHubException("Error: Invalid tenant configuration");
			}
			return new KubernetesClient();
		} catch (Exception e) {
			log.error("Error: Invalid plugin endpoints");
			throw new AttestationHubException("Error: Invalid plugin endpoints", e);
		}
	}
}
