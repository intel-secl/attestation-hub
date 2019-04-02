/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.attestationhub.setup;

import com.intel.dcsg.cpg.crypto.Aes128;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.setup.AbstractSetupTask;
import javax.crypto.SecretKey;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author rawatar
 *
 */
public class CreateDataEncryptionKey extends AbstractSetupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateDataEncryptionKey.class);

    public String getDataEncryptionKeyBase64() {
        return getConfiguration().get(Constants.ATTESTATION_HUB_DATA_ENCRYPTION_KEY);
    }

    public void setDataEncryptionKeyBase64(String dekBase64) {
        getConfiguration().set(Constants.ATTESTATION_HUB_DATA_ENCRYPTION_KEY, dekBase64);
    }

    @Override
    public void configure() throws Exception { }

    @Override
    public void validate() throws Exception {
        String dataEncryptionKeyBase64 = getDataEncryptionKeyBase64();
        if (dataEncryptionKeyBase64 == null || dataEncryptionKeyBase64.isEmpty()) {
            validation("Data encryption key is not configured");
        } else if (!Base64.isBase64(dataEncryptionKeyBase64)) {
            validation("Data encryption key is not formatted correctly");
        }
    }

    @Override
    public void execute() throws Exception {
        System.out.println(String.format("Generating data encryption key %s...", Constants.ATTESTATION_HUB_DATA_ENCRYPTION_KEY));
        SecretKey dek = Aes128.generateKey();
        String dekBase64 = Base64.encodeBase64String(dek.getEncoded());
        setDataEncryptionKeyBase64(dekBase64);
    }
}
