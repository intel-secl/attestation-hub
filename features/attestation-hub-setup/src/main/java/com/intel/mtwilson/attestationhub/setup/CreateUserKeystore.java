/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.attestationhub.setup;

import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.dcsg.cpg.x509.X509Util;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.client.jaxrs.CaCertificates;
import com.intel.mtwilson.privacyca.v2.model.CaCertificateFilterCriteria;
import com.intel.mtwilson.shiro.ShiroUtil;
import com.intel.mtwilson.util.ResourceFinder;
import org.apache.commons.io.IOUtils;

import java.io.*;
import com.intel.mtwilson.setup.AbstractSetupTask;

import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

import static com.intel.mtwilson.attestationhub.common.Constants.TRUSTSTORE_PASSWORD;

/**
 *
 * @author rawatar
 */
public class CreateUserKeystore extends AbstractSetupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateUserKeystore.class);

    private String url;
    private String username;
    private String password;
    private String bearerToken;
    private KeyStore trustStore;
    private File trustStoreFile;
    private final String trustStorePath = Folders.configuration()+"/truststore.";
    private static final String BEARER_TOKEN = "BEARER_TOKEN";
    private final String caCertPath = Folders.configuration() + File.separator + "cms-ca-cert.pem";

    @Override
    protected void configure() throws Exception {

        username = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_SERVICE_USERNAME);
        if (username == null || username.isEmpty()) {
            configuration("Attestation Hub admin username is not set");
        }

        password = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_SERVICE_PASSWORD);
        if (password == null || password.isEmpty()) {
            configuration("Attestation Hub admin password is not set");
        }

        bearerToken = System.getenv(BEARER_TOKEN);
        if (bearerToken == null || bearerToken.isEmpty()){
            configuration("BEARER_TOKEN cannot be empty");
        }

        url = AttestationHubConfigUtil.get(Constants.MTWILSON_API_URL);
        if (url == null || url.isEmpty()) {
            configuration("MtWilson API URL is not set");
        }

        String extension = "p12";
        if (KeyStore.getDefaultType().equalsIgnoreCase("JKS")) {
            extension = "jks";
        }

        String trustStoreFileName = trustStorePath + extension;
        trustStoreFile = new File(trustStoreFileName);
        trustStore = loadTrustStore();
    }

    @Override
    protected void validate() throws Exception {

        List<String> aliases = Collections.list(trustStore.aliases());
        String tag = String.format("(%s)", "saml");
        Iterator<String> it = aliases.iterator();
        while (it.hasNext()) {
            String alias = it.next();
            if (!alias.toLowerCase().endsWith(tag)) {
                it.remove();
            }
        }
        if (aliases.size() == 0) {
            validation("Missing MtWilson Saml certificate");
        }
    }

    @Override
    protected void execute() throws Exception {

        TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustStoreFile, TRUSTSTORE_PASSWORD).build();
        Properties clientConfiguration = new Properties();
        clientConfiguration.setProperty(Constants.BEARER_TOKEN, bearerToken);
        try {
            TlsConnection tlsConnection = new TlsConnection(new URL(url), tlsPolicy);
            CaCertificates certClient = new CaCertificates(clientConfiguration, tlsConnection);
            CaCertificateFilterCriteria criteria = new CaCertificateFilterCriteria();
            criteria.domain = "saml";
            String cert = certClient.searchCaCertificatesPem(criteria);
            List<X509Certificate> samlCertificateChain = X509Util.decodePemCertificates(cert);

            X509Certificate samlCertificate = samlCertificateChain.get(0);
            verifySamlCertChain(samlCertificateChain, samlCertificate);
            storeCertificate(samlCertificate, String.format("%s(%s)", samlCertificate.getSubjectX500Principal().getName(), "saml"));
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
        try {
            rootCas.addAll(X509Util.decodePemCertificates(IOUtils.toString(rootCert)));
        } finally {
            rootCert.close();
        }
        ShiroUtil.verifyCertificateChain(samlCertificate, rootCas, intermediateCas);

    }

    private void storeCertificate (X509Certificate certificate, String alias) throws Exception {
        FileOutputStream keystoreFOS = new FileOutputStream(trustStoreFile);
        try {
            trustStore.setCertificateEntry(alias, certificate);
            trustStore.store(keystoreFOS, TRUSTSTORE_PASSWORD.toCharArray());
        } catch (Exception exc) {
            throw new Exception("Error storing certificate in keystore", exc);
        }finally {
            keystoreFOS.close();
        }
    }

    private KeyStore loadTrustStore() throws Exception{
        FileInputStream keystoreFIS = new FileInputStream(trustStoreFile);
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(keystoreFIS, TRUSTSTORE_PASSWORD.toCharArray());
        } catch (Exception exc) {
            throw new Exception("Error loading trust store", exc);
        } finally {
            keystoreFIS.close();
        }
        return keyStore;
    }
}
