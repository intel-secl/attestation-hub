/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.attestationhub.setup;

import com.intel.dcsg.cpg.crypto.*;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.dcsg.cpg.tls.policy.TlsUtil;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.client.jaxrs.CaCertificates;
import com.intel.mtwilson.setup.AbstractSetupTask;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 *
 * @author rawatar
 */
public class CreateUserKeystore extends AbstractSetupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateUserKeystore.class);

    private static final String TLS_PROTOCOL = "TLSv1.2";

    private String user;
    private String password;
    private String keystore;
    private File folder;
    private URL server;
    private Properties properties;

    @Override
    protected void configure() throws Exception {
        try {
            server = new URL(AttestationHubConfigUtil.get(Constants.MTWILSON_API_URL));
        } catch (MalformedURLException e) {
            log.error("Error forming Attestation Service URL", e);
            throw new AttestationHubException(e);
        }
        user = AttestationHubConfigUtil.get(Constants.MTWILSON_API_USER);
        password = AttestationHubConfigUtil.get(Constants.MTWILSON_API_PASSWORD);
        keystore = Folders.configuration() + File.separator + user + ".jks";

        properties = new Properties();
        folder = new File(Folders.configuration());
        properties.setProperty("mtwilson.api.tls.policy.certificate.sha256",
                AttestationHubConfigUtil.get(Constants.MTWILSON_API_TLS));
    }

    @Override
    protected void validate() throws Exception {
        File keystoreFile = new File(folder.getAbsoluteFile() + File.separator + user + ".jks");
        if( !keystoreFile.exists() ) {
            validation("Keystore file was not created");
            return;
        }
    }

    @Override
    protected void execute() throws Exception {
        File keystoreFile = new File(folder.getAbsoluteFile() + File.separator + user + ".jks");
        FileResource resource = new FileResource(keystoreFile);
        URL baseUrl = new URL(server.getProtocol() + "://" + server.getAuthority());
        SimpleKeystore keystore;
        try {
            // create the keystore and a new credential
            keystore = new SimpleKeystore(resource, password); // KeyManagementException
            KeyPair keypair = RsaUtil.generateRsaKeyPair(RsaUtil.MINIMUM_RSA_KEY_SIZE); // NoSuchAlgorithmException
            X509Certificate certificate = RsaUtil.generateX509Certificate(/*"CN="+*/user, keypair, RsaUtil.DEFAULT_RSA_KEY_EXPIRES_DAYS); // GeneralSecurityException
            keystore.addKeyPairX509(keypair.getPrivate(), certificate, user, password); // KeyManagementException
            keystore.save(); // KeyStoreException, IOException, CertificateException
        }
        catch(KeyManagementException | NoSuchAlgorithmException | KeyStoreException | CertificateException e) {
            throw new CryptographyException("Cannot create keystore", e);
        }

        log.debug("URL Protocol: {}", baseUrl.getProtocol());
        if( "https".equals(baseUrl.getProtocol()) ) {
            TlsUtil.addSslCertificatesToKeystore(keystore, baseUrl, TLS_PROTOCOL); //CryptographyException, IOException
        }

        try {
            String[] aliases = keystore.aliases();
            for(String alias : aliases) {
                log.debug("Certificate: "+keystore.getX509Certificate(alias).getSubjectX500Principal().getName());
            }
        }
        catch(KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException | CertificateEncodingException e) {
            log.debug("cannot display keystore: "+e.toString());
        }

        if( !properties.containsKey("mtwilson.api.url") && !properties.containsKey("mtwilson.api.baseurl") ) {
            properties.setProperty("mtwilson.api.url", server.toExternalForm());
        }

        try {

            CaCertificates certClient = new CaCertificates(properties);
            //X509Certificate rootCertificate = certClient.retrieveCaCertificate("root");
            X509Certificate samlCertificate = certClient.retrieveCaCertificate("saml");
            //X509Certificate privacyCertificate = certClient.retrieveCaCertificate("privacy");

            //log.debug("Adding CA Certificate with alias {} from server {}", rootCertificate.getSubjectX500Principal().getName(), server.getHost());
            //keystore.addTrustedCaCertificate(rootCertificate, rootCertificate.getSubjectX500Principal().getName());

            //log.debug("Adding Privacy CA Certificate with alias {} from server {}", privacyCertificate.getSubjectX500Principal().getName(), server.getHost());
            //keystore.addTrustedCaCertificate(privacyCertificate, privacyCertificate.getSubjectX500Principal().getName());

            if (samlCertificate.getBasicConstraints() == -1) { // -1 indicates the cert is not a CA cert
                log.debug("Adding SAML Certificate with alias {} from server {}", samlCertificate.getSubjectX500Principal().getName(), server.getHost());
                keystore.addTrustedSamlCertificate(samlCertificate, samlCertificate.getSubjectX500Principal().getName());

            } else {
                log.debug("Adding SAML Certificate as CA cert with alias {} from server {}", samlCertificate.getSubjectX500Principal().getName(), server.getHost());
                keystore.addTrustedCaCertificate(samlCertificate, samlCertificate.getSubjectX500Principal().getName());
            }
        } catch (Exception ex) {
            log.error("Error during retrieval of certificates for writing to the key store.", ex);
        }

        try {
            keystore.save();
        }
        catch(KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new CryptographyException("Cannot save keystore to resource: "+e.toString(), e);
        }
    }
}
