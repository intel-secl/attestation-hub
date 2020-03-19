/*
 * Copyright (C) 2020 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.kubernetes.client;


import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.core.common.utils.AASTokenFetcher;
import com.intel.mtwilson.jaxrs2.client.WLSClient;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Properties;

import static com.intel.mtwilson.attestationhub.common.Constants.TRUSTSTORE_PASSWORD;

public class WLSApiUtil {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WLSApiUtil.class);
    private static String aasBearerToken;

    public String getVmReportByVmID(String instanceID) throws AttestationHubException {
        log.info("Fetching VM report for vm : {}", instanceID);
        String vmReport;
        try {
            String trustStoreFileName = Folders.configuration() + File.separator + "truststore.p12";
            TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustStoreFileName, TRUSTSTORE_PASSWORD).build();
            TlsConnection tlsConnection = new TlsConnection(new URL(AttestationHubConfigUtil.get(Constants.WLS_API_URL)), tlsPolicy);
            Properties properties = new Properties();
            properties.setProperty("bearer.token", getAASBearerToken());
            vmReport = new WLSClient(properties, tlsConnection)
                    .getVmReportByVmID(instanceID);
            log.debug("VM report fetched: {}", vmReport);
            return vmReport;
        } catch (IOException exc) {
            log.error("Error forming TLS connection policy to get WLS report : {}", exc.getMessage());
            throw new AttestationHubException("Error forming TLS connection policy to get WLS report");
        } catch (Exception exc) {
            if (exc.getMessage().contains("HTTP 400 Bad Request")) {
                return "";
            }
            log.error("Error while fetching WLS report : {}", exc.getMessage());
            throw new AttestationHubException("Error while creating WLS client to get WLS report");
        }
    }

    private String getAASBearerToken() throws AttestationHubException {
        try {
            String trustStoreFileName = Folders.configuration() + File.separator + "truststore.p12";
            TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustStoreFileName, TRUSTSTORE_PASSWORD).build();
            TlsConnection tlsConnection;
            String aasAPIUrl = AttestationHubConfigUtil.get(Constants.AAS_API_URL);
            if ( aasAPIUrl != null && !aasAPIUrl.isEmpty() )
                tlsConnection = new TlsConnection(new URL(aasAPIUrl), tlsPolicy);
            else
                throw new AttestationHubException("AAS api url not defined");
            aasBearerToken = new AASTokenFetcher().updateCachedToken(
                    AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_SERVICE_USERNAME),
                    AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_SERVICE_PASSWORD), tlsConnection,
                    aasBearerToken);
        } catch(IOException exc) {
            log.error("Error getting Bearer token from AAS :" + exc);
            throw new AttestationHubException("Error getting Bearer token from AAS");
        }
        log.info("AAS Bearer Token : {}", aasBearerToken);
        return aasBearerToken;
    }
}
