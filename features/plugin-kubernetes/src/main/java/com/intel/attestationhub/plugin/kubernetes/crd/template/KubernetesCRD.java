/**
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes.crd.template;

/**
 * @author abhishekx.negi@intel.com
 *
 *         Abstract class for CRDs as CRD object follow this structure. This
 *         class will be extended for new CRDs to be created.
 */
public abstract class KubernetesCRD {

	private String apiVersion;
	private String kind;
	protected Metadata metadata;
	protected Spec spec;

	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	protected Metadata getMetadata() {
		return metadata;
	}

	protected abstract void setMetadata(Metadata metadata);

	protected Spec getSpec() {
		return spec;
	}

	protected abstract void setSpec(Spec spec);

}
