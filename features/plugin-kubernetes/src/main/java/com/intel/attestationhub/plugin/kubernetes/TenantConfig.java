/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes;

/**
 * @author abhishekx.negi@intel.com
 *	
 * Tenant configuration class
 */
public class TenantConfig {

	private String pluginApiEndpoint;
	private String tenantName;
	private String bearerToken;
	private String serverKeystore;
	private String serverKeystorePass;
	private String keystoneVersion;
	private String openstackTenantName;
	private String openstackScope;
	private String openstackUsername;
	private String openstackPass;
	private String openstackURI;
	private boolean vmWorkerDisabled;

	private static TenantConfig tenantConfig = null;

	private TenantConfig() {

	}

	public static TenantConfig getTenantConfigObj() {
		if (tenantConfig == null) {
			tenantConfig = new TenantConfig();
			return tenantConfig;
		}
		return tenantConfig;
	}

	public String getOpenstackURI() {
		return openstackURI;
	}

	public void setOpenstackURI(String openstackURI) {
		this.openstackURI = openstackURI;
	}

	public String getKeystoneVersion() {
		return keystoneVersion;
	}

	public void setKeystoneVersion(String keystoneVersion) {
		this.keystoneVersion = keystoneVersion;
	}

	public String getOpenstackTenantName() {
		return openstackTenantName;
	}

	public void setOpenstackTenantName(String openstackTenantName) {
		this.openstackTenantName = openstackTenantName;
	}

	public String getOpenstackUsername() {
		return openstackUsername;
	}

	public void setOpenstackUsername(String openstackUsername) {
		this.openstackUsername = openstackUsername;
	}

	public String getOpenstackPass() {
		return openstackPass;
	}

	public void setOpenstackPass(String openstackPass) {
		this.openstackPass = openstackPass;
	}

	public String getOpenstackScope() {
		return openstackScope;
	}

	public void setOpenstackScope(String openstackScope) {
		this.openstackScope = openstackScope;
	}

	public boolean isVmWorkerDisabled() {
		return vmWorkerDisabled;
	}

	public void setVmWorkerDisabled(boolean vmWorkerDisabled) {
		this.vmWorkerDisabled = vmWorkerDisabled;
	}

	public String getPluginApiEndpoint() {
		return pluginApiEndpoint;
	}

	public void setPluginApiEndpoint(String pluginApiEndpoint) {
		this.pluginApiEndpoint = pluginApiEndpoint;
	}

	public String getTenantName() {
		return tenantName;
	}

	public void setTenantName(String tenantName) {
		this.tenantName = tenantName;
	}

	public String getBearerToken() {
		return bearerToken;
	}

	public void setBearerToken(String bearerToken) {
		this.bearerToken = bearerToken;
	}

	public String getServerKeystore() {
		return serverKeystore;
	}

	public void setServerKeystore(String serverKeystore) {
		this.serverKeystore = serverKeystore;
	}

	public String getServerKeystorePass() {
		return serverKeystorePass;
	}

	public void setServerKeystorePass(String serverKeystorePass) {
		this.serverKeystorePass = serverKeystorePass;
	}

}
