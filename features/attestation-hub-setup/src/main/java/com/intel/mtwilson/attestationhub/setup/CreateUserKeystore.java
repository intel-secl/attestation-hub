/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.attestationhub.setup;

import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.impl.InsecureTlsPolicy;
import com.intel.kms.setup.JettyTlsKeystore;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.client.jaxrs.CaCertificates;
import com.intel.mtwilson.core.common.utils.AASTokenFetcher;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 *
 * @author rawatar
 */
public class CreateUserKeystore extends JettyTlsKeystore {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateUserKeystore.class);

    private String url;
    private final String trustStorePath = Folders.configuration()+"/truststore.";

    @Override
    protected void configure() throws Exception {

        username = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_ADMIN_USERNAME);
        if (username == null || username.isEmpty()) {
            configuration("Attestation Hub admin username is not set");
        }

        password = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_ADMIN_PASSWORD);
        if (password == null || password.isEmpty()) {
            configuration("Attestation Hub admin password is not set");
        }

        url = AttestationHubConfigUtil.get(Constants.MTWILSON_API_URL);
        if (url == null || url.isEmpty()) {
            configuration("Mt Wilson URL is not set");
        }

        super.configure();
    }

    @Override
    protected void validate() throws Exception {
        super.validate();
    }

    @Override
    protected void execute() throws Exception {
        super.execute();

        String extension = "p12";
        if (KeyStore.getDefaultType().equalsIgnoreCase("JKS")) {
            extension = "jks";
        }

        String trustStoreFileName = trustStorePath+extension;

        TlsConnection tlsConnection = new TlsConnection(new URL(url), new InsecureTlsPolicy());
        Properties clientConfiguration = new Properties();
        clientConfiguration.setProperty(Constants.BEARER_TOKEN, new AASTokenFetcher().getAASToken(aasApiUrl, username, password));

        try {
            CaCertificates certClient = new CaCertificates(clientConfiguration, tlsConnection);
            X509Certificate samlCertificate = certClient.retrieveCaCertificate("saml");
            storeCertificate(samlCertificate, String.format("%s(%s)", samlCertificate.getSubjectX500Principal().getName(), "saml"), trustStoreFileName);
        } catch (Exception ex) {
            log.error("Error during retrieval of certificates for writing to the key store.", ex);
        }
    }

    private void storeCertificate (X509Certificate certificate, String alias, String trustStoreFileName) throws Exception {
        KeyStore keystore = loadTrustStore(trustStoreFileName);
        FileOutputStream keystoreFOS = new FileOutputStream(trustStoreFileName);
        try {
            keystore.setCertificateEntry(alias, certificate);
            keystore.store(keystoreFOS, "changeit".toCharArray());
        } catch (Exception exc) {
            throw new Exception("Error storing certificate in keystore", exc);
        }finally {
            keystoreFOS.close();
        }
    }

    private KeyStore loadTrustStore(String trustStoreFileName) throws Exception{
        FileInputStream keystoreFIS = new FileInputStream(trustStoreFileName);
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(keystoreFIS, "changeit".toCharArray());
        } catch (Exception exc) {
            throw new Exception("Error loading trust store", exc);
        } finally {
            keystoreFIS.close();
        }
        return keyStore;
    }
}
