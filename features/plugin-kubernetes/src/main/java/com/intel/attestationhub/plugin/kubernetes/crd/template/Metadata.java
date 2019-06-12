/**
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes.crd.template;

/**
 * @author abhishekx.negi@intel.com
 *
 *         Abstract class for CRD object metadata. This class will be extended
 *         for new CRD object's metadata.
 */
public abstract class Metadata {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
