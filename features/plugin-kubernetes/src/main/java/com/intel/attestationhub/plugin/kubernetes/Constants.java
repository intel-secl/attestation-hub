/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes;

/**
 * @author abhishekx.negi@intel.com
 *
 */
public class Constants {
	public class Tenant {

		public static final String API_ENDPOINT = "api.endpoint";
		public static final String TENANT_NAME = "tenant.name";
		public static final String KUBERNETES_API_CLIENT_KEYSTORE = "kubernetes.client.keystore";
		public static final String KUBERNETES_API_CLIENT_KEYSTORE_PASSWORD = "kubernetes.client.keystore.password";
		public static final String KUBERNETES_API_SERVER_KEYSTORE = "kubernetes.server.keystore";
		public static final String KUBERNETES_API_SERVER_KEYSTORE_PASSWORD = "kubernetes.server.keystore.password";
		public static final String TENANT_KUBERNETES_KEYSTORE_CONFIG = "tenant.kubernetes.keystore.config";
		public static final String KEYSTONE_VERSION = "keystone.version";
		public static final String OPENSTACK_TENANT_NAME = "openstack.tenant.name";
		public static final String OPENSTACK_SCOPE = "openstack.scope";
		public static final String OPENSTACK_USERNAME = "openstack.username";
		public static final String OPENSTACK_PASS = "openstack.pass";
		public static final String VM_WORKER_DISABLED = "vm.worker.disabled";
		public static final String OPENSTACK_URI = "openstack.uri";

	}

	public static class Plugin {
		public static final String URL_TYPE = "API";
		public static final String URL_HOSTATTRIBUTES = "hostattributes";
		public static final String PATH = "/apis/crd.isecl.intel.com/v1beta1/namespaces/default/";
		public static final String NODE_DETAILS_PATH = "/api/v1/nodes";
		public static final String SLASH = "/";
		public static final String METADATA = "metadata";
		public static final String NAME = "name";
		public static final String KIND = "kind";
		public static final String NULL = "null";
		public static final String RESOURCE_VERSION = "resourceVersion";
		public static final Integer ZERO = 0;
		public static final String INSTANCE_TYPE = "PKCS12";
		public static final String SLASH_COMMA = "\"";
		public static final String EMPTY_STRING = "";
		public static final String SPEC = "spec";
		public static final String HOSTLIST = "hostList";
		public static final String HOSTNAME = "hostName";
		public static final String TRUSTED = "trusted";
		public static final String SIGNED_TRUST_REPORT = "signedTrustReport";
		public static final String VALID_TO = "validTo";
	}
	
	public static class Report {
		public static final String HOSTNAME = "hostname";
		public static final String ASSET_TAGS = "asset_tags";
		public static final String VALID_TO = "valid_to";
	}
}

