/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova;

import java.util.concurrent.TimeUnit;

public class Constants {

    public static final String AUTH_ENDPOINT = "auth.endpoint";
    public static final String KEYSTONE_VERSION = "auth.version";
    public static final String USERNAME = "user.name";
    public static final String PASSWORD = "user.password";
    public static final String TENANT_NAME = "tenant.name";
    public static final String DOMAIN_NAME = "domain.name";
    public static final String AUTH_TOKEN = "X-AUTH-TOKEN";
    public static final int MAX_RETRIES_DUE_TO_CONFLICTS = 3;
    public static final long CONFLICT_RETRY_DELAY_IN_MILLIS = 200L;
    /**
     * If updating the traits for 5 hosts fail in a batch of hosts(any number), then we fail the entire request without
     * attempting to process the other hosts. If the number of failed calls do not reach the threshold, we continue
     * to process traits for other hosts.
     */
    public static final int NO_OF_FAILED_CALLS_BEFORE_FAILING_BATCH = 5;
    public static final int CONNECTION_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);
    public static final int SOCKET_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(60);

    public static final String PLACEMENT_API_MICROVERSION_VALUE = "placement 1.21";
    public static final String OPENSTACK_API_MICROVERSION_HEADER = "OpenStack-API-Version";
    public static final String KEYSTONE_AUTH_TOKEN_HEADER_KEY = "X-Subject-Token";

    public static final String RESOURCE_PATH_V3_AUTH_TOKEN = "/v3/auth/tokens";

    public static final String RESOURCE_PATH_TRAITS = "/traits";
    public static final String RESOURCE_PATH_RESOURCE_PROVIDERS = "/resource_providers/";
    public static final String RESOURCE_PATH_RESOURCE_PROVIDERS_NAME_QUERY = "/resource_providers?name=";

    public static final String CIT_TRAIT_PREFIX = "CUSTOM_ISECL";
    public static final String AT_PREFIX = "_AT_";
    public static final String HAS_PREFIX = "_HAS_";
    public static final String CIT_TRUSTED_TRAIT = CIT_TRAIT_PREFIX + "_TRUSTED";
    public static final String OPENSTACK_TRAITS_DELIMITER = "_";

}
