/**
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes.crd;

public class Constants {
	public static final String HOSTNAME = "hostname";
	public static final String VALID_TO = "valid_to";
	public static final String LOCATION = "location";
	public static final String GEOLOCATION = "geolocation";
	public static final String PLATFORM = "platform";
	public static final String TRUSTED = "trusted";
	public static final String TAB = "Tab";
	public static final String API_VERSION = "isecl.intel.com/v1beta1";
	public static final String OBJECT = "object";
	public static final String CIT = "isecl";
	public static final String ASSET_TAGS = "asset_tags";
	public static final Integer ZERO = 0;
	public static final Integer ONE = 1;
	public static final String HYPHEN = "-";
	public static final String DOT = ".";
	public static final String LOCATION_CRD = "GeolocationCrd";
	public static final String PLATFORM_CRD = "PlatformCrd";
	public static final String EMPTY_ASSET_TAGS = "{}";
	public static final boolean CREATE_PLATFORM_CRD = true;
	public static final boolean CREATE_GEOLOCATION_CRD = true;
	public static final String SPACE = " ";
	public static final String ASSET_LABEL_REGEX = "(?:[a-zA-Z0-9_\\/\\.-]+)";
	public static final String ASSET_VALUE_REGEX = "(?:[a-zA-Z0-9_\\.-]+)";
}
