/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.attestationhub.setup;

import java.io.File;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.util.exec.ExecUtil;

public class TrustReportEncryptionKey extends AbstractSetupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrustReportEncryptionKey.class);

    private static final String PUBLIC_KEY_PATH = Folders.configuration() + File.separator + Constants.PUBLIC_KEY_FILE;
    private static final String PRIVATE_KEY_PATH = Folders.configuration() + File.separator
	    + Constants.PRIVATE_KEY_FILE;

    @Override
    protected void configure() throws Exception {
    }

    @Override
    protected void validate() throws Exception {
	File f = new File(PRIVATE_KEY_PATH);
	if (!f.exists()) {
	    validation("Private key is necessary for encrypting trust report");
	}
	f = new File(PUBLIC_KEY_PATH);
	if (!f.exists()) {
	    validation("Public key necessary for sharing with tenants");
	}
    }

    @Override
    protected void execute() throws Exception {
	String command = "openssl genrsa 3072 > " + Folders.configuration() + File.separator + "TEMP"
		+ Constants.PRIVATE_KEY_FILE;
	ExecUtil.executeQuoted("/bin/bash", "-c", command);
	command = "openssl rsa -in "
		+ (Folders.configuration() + File.separator + "TEMP" + Constants.PRIVATE_KEY_FILE)
		+ " -outform PEM -pubout -out "
		+ (Folders.configuration() + File.separator + Constants.PUBLIC_KEY_FILE);
	ExecUtil.executeQuoted("/bin/bash", "-c", command);
	//Convert private key to PKCS8
	String tempFile = Folders.configuration() + File.separator + "TEMP"
		+ Constants.PRIVATE_KEY_FILE;
	String priKeyFile = Folders.configuration() + File.separator 
		+ Constants.PRIVATE_KEY_FILE;
	
	command = "openssl pkcs8 -topk8 -inform PEM -outform DER -in "+ tempFile +" -out "+ priKeyFile +"  -nocrypt";
	ExecUtil.executeQuoted("/bin/bash", "-c", command);
	File f = new File(tempFile);
	f.delete();

    }
}
