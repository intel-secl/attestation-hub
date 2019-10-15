/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.attestationhub.setup;

import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.dcsg.cpg.x509.X509Util;
import com.intel.kms.setup.JettyTlsKeystore;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.client.jaxrs.CaCertificates;
import com.intel.mtwilson.core.common.utils.AASTokenFetcher;
import com.intel.mtwilson.privacyca.v2.model.CaCertificateFilterCriteria;
import com.intel.mtwilson.shiro.ShiroUtil;
import com.intel.mtwilson.util.ResourceFinder;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author rawatar
 */
public class CreateUserKeystore extends JettyTlsKeystore {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateUserKeystore.class);

    private String url;
    private String aasApiUrl;
    private final String trustStorePath = Folders.configuration()+"/truststore.";
    private final String caCertPath = Folders.configuration() + File.separator + "cms-ca.cert";

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

        aasApiUrl = AttestationHubConfigUtil.get(Constants.AAS_API_URL);
        if (aasApiUrl == null || aasApiUrl.isEmpty()) {
            configuration("AAS URL is not set");
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

        TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustStoreFileName, "changeit").build();
        Properties clientConfiguration = new Properties();
        TlsConnection tlsConnection = new TlsConnection(new URL(aasApiUrl), tlsPolicy);
        clientConfiguration.setProperty(Constants.BEARER_TOKEN, new AASTokenFetcher().getAASToken(username, password, tlsConnection));

        try {
            tlsConnection = new TlsConnection(new URL(url), tlsPolicy);
            CaCertificates certClient = new CaCertificates(clientConfiguration, tlsConnection);
            CaCertificateFilterCriteria criteria = new CaCertificateFilterCriteria();
            criteria.domain = "saml";
            String cert = certClient.searchCaCertificatesPem(criteria);
            List<X509Certificate> samlCertificateChain = X509Util.decodePemCertificates(cert);

            X509Certificate samlCertificate = samlCertificateChain.get(0);
            verifySamlCertChain(samlCertificateChain, samlCertificate);
            storeCertificate(samlCertificate, String.format("%s(%s)", samlCertificate.getSubjectX500Principal().getName(), "saml"), trustStoreFileName);
        } catch(GeneralSecurityException e) {
            log.error("Error verifying signature: ", e);
        } catch(Exception ex) {
            log.error("Error during retrieval of certificates for writing to the key store.", ex);
        }
    }

    private void verifySamlCertChain(List<X509Certificate> samlCertificateChain, X509Certificate samlCertificate) throws IOException, GeneralSecurityException {
        ArrayList<Certificate> intermediateCas = new ArrayList<>();
        intermediateCas.add(samlCertificateChain.get(1));

        ArrayList<Certificate> rootCas = new ArrayList<>();
        InputStream rootCert = new FileInputStream(ResourceFinder.getFile(caCertPath));
        rootCas.addAll(X509Util.decodePemCertificates(IOUtils.toString(rootCert)));

        ShiroUtil.verifyCertificateChain(samlCertificate, rootCas, intermediateCas);
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
