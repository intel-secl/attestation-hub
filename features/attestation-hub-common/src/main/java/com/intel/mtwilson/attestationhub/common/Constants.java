/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.attestationhub.common;

public class Constants {
    public static final String ATTESTATION_HUB_PROPRRTIES_FILE_NAME = "attestation-hub.properties";
    public static final String ATTESTATION_HUB_DATABASE_NAME = "attestation_hub_pu";
    public static final String ATTESTATION_HUB_DB_USERNAME = "attestation-hub.db.username";
    public static final String ATTESTATION_HUB_DB_URL = "attestation-hub.db.url";
    public static final String ATTESTATION_HUB_DB_DRIVER = "attestation-hub.db.driver";
    public static final String ATTESTATION_HUB_DB_PASSWORD = "attestation-hub.db.password";
    public static final String ATTESTATION_HUB_DATA_ENCRYPTION_KEY = "attestation-hub.dek";
    public static final String ATTESTATION_HUB_ADMIN_USERNAME = "attestation-hub.admin.username";
    public static final String ATTESTATION_HUB_ADMIN_PASSWORD = "attestation-hub.admin.password";
    public static final String ATTESTATION_HUB_KEYSTORE_PASSWORD = "keystore.password";

    public final static String BEARER_TOKEN = "bearer.token";
    public final static String AAS_API_URL = "aas.api.url";
    public final static String CMS_BASE_URL = "cms.base.url";
    public static final String MTWILSON_API_URL = "mtwilson.api.url";
    public static final String MTWILSON_API_USER = "mtwilson.api.username";
    public static final String MTWILSON_API_PASSWORD = "mtwilson.api.password";
    public static final String MTWILSON_API_TLS = "mtwilson.api.tls.policy.certificate.sha384";
    public static final String ATTESTATION_HUB_POLL_INTERVAL = "attestation-hub.poll.interval";
    public static final String ATTESTATION_HUB_SAML_TIMEOUT = "attestation-hub.saml.timeout";
    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String PUBLIC_KEY_FILE = "hub_public_key.pem";
    public static final String PRIVATE_KEY_FILE = "hub_private_key.pem";
    public static final String SAML_TAG = "TAG";
    public static final String TRUST_TAG = "TRUST";
    public static final String FEATURE_TAG = "FEATURE";
    public static final String PLUGIN_PROVIDER = "plugin.provider";
    public static final String NAME_REGEX = "[a-zA-Z0-9_.-]+";
    public static final String XSS_REGEX="(?i)^.*(<|>|Redirect|script|alert).*$";
}
