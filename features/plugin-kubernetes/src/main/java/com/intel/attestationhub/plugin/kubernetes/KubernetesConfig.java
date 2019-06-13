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
				case Tenant.TENANT_KUBERNETES_KEYSTORE_CONFIG:
					tenantConfig.setTenantKeystoreConfig(PluginProperty.getValue());
					break;
				case Tenant.VM_WORKER_ENABLED:
					tenantConfig.setVmWorkerEnabled(PluginProperty.getValue());
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
				}

			}
			if (StringUtils.isBlank(tenantConfig.getTenantName())
					|| StringUtils.isBlank(tenantConfig.getPluginApiEndpoint())
					|| StringUtils.isBlank(tenantConfig.getTenantKeystoreConfig())) {
				log.error("Error: Invalid tenant configuration");
				throw new AttestationHubException("Error: Invalid tenant configuration");
			}
			// If VM worker enabled then checking required openstack configuration
			if (Constants.Plugin.STRING_TRUE.equals(tenantConfig.isVmWorkerEnabled())) {
				if (StringUtils.isBlank(tenantConfig.getKeystoneVersion())
						|| StringUtils.isBlank(tenantConfig.getOpenstackTenantName())
						|| StringUtils.isBlank(tenantConfig.getOpenstackScope())
						|| StringUtils.isBlank(tenantConfig.getOpenstackUsername())
						|| StringUtils.isBlank(tenantConfig.getOpenstackPass())
						|| StringUtils.isBlank(tenantConfig.getOpenstackURI())) {
					log.error("Error: Missing openstack or mtwilson parameters");
					throw new AttestationHubException("Error: Missing openstack or mtwilson parameters");
				}
			}
			// Tenant keystore configuration validation
			loadKeystore(tenantConfig.getTenantKeystoreConfig());
			return new KubernetesClient();
		} catch (Exception e) {
			log.error("Error: Invalid plugin endpoints");
			throw new AttestationHubException("Error: Invalid plugin endpoints", e);
		}
	}

	/**
	 * It validates the tenant configuration parameters- clientKeystore,
	 * clientKeystorePass, serverKeystore and serverKeystorepass.
	 *
	 * When tenant's kubernetes keystore configuration file doesn't exists
	 * Output: {
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Tenant's kubernetes keystore
	 *                 config properties file does not exist" }
	 * 
	 *                 When required keystores fields is missing or is empty
	 *                 Output: { "Logged error message" : "Error: Invalid
	 *                 tenant's kubernetes keystore configuration"}
	 * @exception: AttestationHubException
	 *                 with the message, Error: Invalid tenant's kubernetes
	 *                 keystore configuration }
	 *
	 * @param keystoreFilePath
	 *            Path to the properties file for tenant's kubernetes keystore
	 *            configuration file
	 * 
	 */
	private void loadKeystore(String keystoreFilePath) throws AttestationHubException {
		Properties prop = new Properties();
		try(InputStream input = new FileInputStream(keystoreFilePath)) {
			prop.load(input);
		} catch (Exception e) {
			log.error("Error: Tenant's kubernetes keystore config properties file does not exist");
			throw new AttestationHubException(
					"Error: Tenant's kubernetes keystore config properties file does not exist");
		}
		tenantConfig.setClientKeystore(prop.getProperty(Tenant.KUBERNETES_API_CLIENT_KEYSTORE));
		tenantConfig.setClientKeystorePass(prop.getProperty(Tenant.KUBERNETES_API_CLIENT_KEYSTORE_PASSWORD));
		tenantConfig.setServerKeystore(prop.getProperty(Tenant.KUBERNETES_API_SERVER_KEYSTORE));
		tenantConfig.setServerKeystorepass(prop.getProperty(Tenant.KUBERNETES_API_SERVER_KEYSTORE_PASSWORD));

		if (StringUtils.isBlank(tenantConfig.getClientKeystore())
				|| StringUtils.isBlank(tenantConfig.getClientKeystorePass())
				|| StringUtils.isBlank(tenantConfig.getServerKeystore())
				|| StringUtils.isBlank(tenantConfig.getServerKeystorepass())) {
			log.error("Error: Invalid tenant's kubernetes keystore configuration");
			throw new AttestationHubException("Error: Invalid tenant's kubernetes keystore configuration");
		}
	}
}
