/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import com.intel.attestationhub.plugin.kubernetes.Constants.Plugin;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

/**
 * @author abhishekx.negi@intel.com
 *
 */
public class KubernetesCertificateAuthenticator {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
			.getLogger(KubernetesCertificateAuthenticator.class);
	TenantConfig tenant = TenantConfig.getTenantConfigObj();

	/**
	 * Creating a connection between Kubernetes Master and the Attestation Hub
	 * machine after reading and validating the client keystore and server
	 * keystore.
	 * <p>
	 * These keystores are stored on the Attestation Hub machine during a tenant
	 * configuration. Path of the stored keystores are provided during the
	 * tenant registration in createTenant API.
	 * 
	 * Sample input while tenant registration { "name": "kubernetes",
	 * "properties": [{ "key": "api.endpoint", "value":
	 * "https://k8s.master.com:6443" }, { "key": "tenant.name", "value":
	 * "14_feb_test" }, { "key": "plugin.provider", "value":
	 * "com.intel.attestationhub.plugin.kubernetes.KubernetesPluginImpl" },{ "key":
	 * "kubernetes.api.bearer.token", "value":
	 * "6O0UKQbBOviU9qjE8yvCdghT3pbslA1AilCbHBvGBCgwmim9qsk4RqAnmC6uyEFb" }, {
	 * "key": "kubernetes.server.keystore", "value":
	 * "/opt/attestation-hub/configuration/athub12_k8s_trust.jks" }, { "key":
	 * "kubernetes.server.keystore.password", "value":
	 * "UfCPZ9zRHFn6FyA0yE4Kfogl2G4RwCdJm5PhNQGJtgSVLVVFwYde0PnVEZj57TgG" }] }
	 *
	 * When keystore files are not present in the defined location or password
	 * is incorrect Output: { "Logged error message":"Error in reading or
	 * loading keystore"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error in reading or loading keystore"
	 *                 } When error comes in closing the keystore files Output:
	 *                 { "Logged error message":"Error in closing keystore file"
	 * @exception: AttestationHubException
	 *                 with the message, "Error in closing keystore file" } In
	 *                 case of unable to initialize SSLContext for
	 *                 CloseableHttpClient Output: { "Logged error
	 *                 message":"Error in initiating SSLContext"
	 * @exception: AttestationHubException
	 *                 with the message, "Error in initiating SSLContext" }
	 * 
	 * @param serverKeystore
	 *            the path where the server keystore is stored on the machine
	 * @param serverKeystorePass
	 *            password for the server keystore
	 * @return a closeableHttpClient to connect Attestation Hub to Kubernetes
	 *         Master to create CRD objects
	 * 
	 */
	protected CloseableHttpClient getHttpClient() throws AttestationHubException {
		KeyStore truststore = null;
		try {
			truststore = KeyStore.getInstance(Plugin.INSTANCE_TYPE);
		} catch (KeyStoreException e) {
			log.error("Error in getting truststore instance", e);
			throw new AttestationHubException("Error in getting truststore instance", e);
		}
		File truststoreFile = new File(tenant.getServerKeystore());
		FileInputStream fisTruststore = null;
		try {
			fisTruststore = new FileInputStream(truststoreFile);
			truststore.load(fisTruststore, tenant.getServerKeystorePass().toCharArray());
		} catch (NoSuchAlgorithmException | CertificateException | IOException e) {
			log.error("Error in reading or loading keystore", e);
			throw new AttestationHubException("Error in reading or loading keystore", e);
		} finally {
			if (fisTruststore != null) {
				try {
					fisTruststore.close();
				} catch (IOException e) {
					log.error("Error in closing truststore file", e);
					throw new AttestationHubException("Error in closing truststore file", e);
				}
			}
		}
		SSLContext sslcontext = null;
		try {
			sslcontext = SSLContexts.custom().loadTrustMaterial(truststore).build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			log.error("Error in initiating SSLContext", e);
			throw new AttestationHubException("Error in initiating SSLContext", e);
		}

		SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslcontext,
				new String[] { "TLSv1.2" }, null, SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER);
		return HttpClients.custom().setSSLSocketFactory(sslSocketFactory).build();
	}
}
