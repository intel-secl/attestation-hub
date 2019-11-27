/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.mtwclient;

import com.intel.attestationhub.api.MWHost;
import com.intel.dcsg.cpg.extensions.Extensions;
import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.core.common.utils.AASTokenFetcher;
import com.intel.mtwilson.flavor.client.jaxrs.Reports;
import com.intel.mtwilson.flavor.rest.v2.model.ReportCollection;
import com.intel.mtwilson.flavor.rest.v2.model.Report;
import com.intel.mtwilson.flavor.rest.v2.model.Host;
import com.intel.mtwilson.flavor.rest.v2.model.HostCollection;
import com.intel.mtwilson.flavor.rest.v2.model.HostLocator;
import com.intel.mtwilson.flavor.client.jaxrs.Hosts;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.controller.AhHostJpaController;
import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.attestationhub.service.PersistenceServiceFactory;
import com.intel.mtwilson.flavor.rest.v2.model.HostFilterCriteria;
import com.intel.mtwilson.flavor.rest.v2.model.ReportFilterCriteria;
import com.intel.mtwilson.supplemental.saml.TrustAssertion;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyCreator;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.ws.rs.NotAuthorizedException;
import java.io.File;
import java.net.ConnectException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.intel.mtwilson.attestationhub.common.Constants.TRUSTSTORE_PASSWORD;

@SuppressWarnings("deprecation")
public class AttestationServiceClient {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttestationServiceClient.class);

    private static Properties mtwProperties = new Properties();
    private static Properties mtwPropertiesForverification = new Properties();
    private static AttestationServiceClient attestationServiceClient = null;
    private static String aasBearerToken;


    private AttestationServiceClient() throws AttestationHubException {
        Extensions.register(TlsPolicyCreator.class,
                com.intel.mtwilson.tls.policy.creator.impl.CertificateTlsPolicyCreator.class);
        populateAttestationServiceProperties();
    }

    public static AttestationServiceClient getInstance() throws AttestationHubException {
        if (attestationServiceClient == null) {
            attestationServiceClient = new AttestationServiceClient();
        }
        return attestationServiceClient;
    }

    public Map<String, MWHost> fetchHostAttestations(List<Host> hosts) throws AttestationHubException {
        if (mtwProperties == null) {
            throw new AttestationHubException("Configuration parameters for MTW client are not initialized");
        }

        if (hosts == null || hosts.size() == 0) {
            log.info("No hosts passed to the method to fetch the attestations");
            return null;
        }
        log.info("Fetching host attestations");
        Map<String, MWHost> hostIdToMwHostMap = new HashMap<>(hosts.size());
        Reports hostReports = MtwClientFactory.getHostReports(mtwProperties);

        for (Host host : hosts) {
            String hostId = host.getId().toString();
            log.info("Retrieving attestation for host: {}", hostId);
            ReportFilterCriteria criteria = new ReportFilterCriteria();
            criteria.hostName = host.getHostName();
            criteria.limit = 1;
            ReportCollection searchHostReports;
            String saml;
            try {
                searchHostReports = hostReports.search(criteria);
                saml = hostReports.searchSamlReports(criteria);
            } catch (Exception e) {
                log.error("Unable to get host attestations or saml report for host with ID={} and name={}", host.getId().toString(),
                        host.getHostName(), e);
                if (e instanceof NotAuthorizedException) {
                    updateTokenCache();
                    throw new AttestationHubException("Not authorized to connect to attestation service", e);
                }
                if (e instanceof ConnectException) {
                    throw new AttestationHubException("Cannot connect to attestation service", e);
                }
                continue;
            }
            if (searchHostReports != null && searchHostReports.getReports() != null
                    && searchHostReports.getReports().size() > 0) {
                Report hostReport = searchHostReports.getReports().get(0);
                populateMwHost(host, hostReport, hostIdToMwHostMap, saml);
            }
        }

        log.info("Returning the hosts and host attestations");
        return hostIdToMwHostMap;
    }

    public List<Host> fetchHosts() throws AttestationHubException {
        if (mtwProperties == null) {
            throw new AttestationHubException("Configuration parameters for MTW client are not initialized");
        }
        log.info("Fetching ALL hosts from Attestation Service");
        List<Host> hosts = null;
        Hosts hostsService = MtwClientFactory.getHostsClient(mtwProperties);
        HostFilterCriteria criteria = new HostFilterCriteria();
        criteria.filter = false;
        HostCollection objCollection = null;
        try {
            objCollection = hostsService.search(criteria);
        } catch (Exception e) {
            log.error("Error while fetching hosts from Attestation Service as part of poller", e);
            if (e instanceof NotAuthorizedException) {
                updateTokenCache();
                throw new AttestationHubException("Not authorized to connect to attestation service", e);
            }
            if (e instanceof ConnectException) {
                throw new AttestationHubException("Cannot connect to attestation service", e);
            }
            throw new AttestationHubException(e);
        }
        if (objCollection != null && objCollection.getHosts() != null && objCollection.getHosts().size() > 0) {
            hosts = objCollection.getHosts();
            log.info("Call to MTW get hosts returned {} hosts", hosts.size());
        }
        log.info("Returning hosts list");
        return hosts;
    }

    public Map<String, MWHost> fetchHostAttestations(String lastDateTimeFromLastRunFile)
            throws AttestationHubException {
        if (mtwProperties == null) {
            throw new AttestationHubException("Configuration parameters for MTW client are not initialized");
        }

        if (StringUtils.isBlank(lastDateTimeFromLastRunFile)) {
            log.info("No last run time to fetch the attestations");
            return null;
        }

        log.info("Fetching host attestations added since {}", lastDateTimeFromLastRunFile);
        Map<String, MWHost> hostIdToMwHostMap = new HashMap<>();
        Reports reportsClient = MtwClientFactory.getHostReports(mtwProperties);
        Hosts hostsClient = MtwClientFactory.getHostsClient(mtwProperties);

        ReportFilterCriteria criteria = new ReportFilterCriteria();
        criteria.fromDate = lastDateTimeFromLastRunFile;
        criteria.latestPerHost = "true";
        ReportCollection reports;
        try {
            reports = reportsClient.search(criteria);
        } catch (Exception e) {
            log.error("Unable to get host attestations or saml for from date : {}", lastDateTimeFromLastRunFile, e);
            if (e instanceof NotAuthorizedException) {
                updateTokenCache();
                throw new AttestationHubException("Not authorized to connect to attestation service", e);
            }
            if (e instanceof ConnectException) {
                throw new AttestationHubException("Cannot connect to attestation service", e);
            }
            return null;
        }

        if (reports != null && reports.getReports() != null && !reports.getReports().isEmpty()) {
            List<Report> reportList = reports.getReports();
            for (Report report : reportList) {
                // retrieve attestation service host record
                HostLocator hostLocator = new HostLocator();
                hostLocator.id = report.getHostId();
                Host asHost = hostsClient.retrieve(hostLocator);

                if (asHost != null) {
                    // retrieve saml record for host
                    ReportFilterCriteria samlCriteria = new ReportFilterCriteria();
                    samlCriteria.hostId = hostLocator.id.toString();
                    samlCriteria.latestPerHost = "true";
                    samlCriteria.limit = 1;
                    String saml = reportsClient.searchSamlReports(samlCriteria);

                    if (saml != null) {
                        populateMwHost(asHost, report, hostIdToMwHostMap, saml);
                    }
                }
            }
        }
        log.info("Returning the hosts and host attestations returned from MTW : {}", hostIdToMwHostMap.size());
        return hostIdToMwHostMap;
    }

    public void updateHostsForSamlTimeout() throws AttestationHubException {
        log.info("updating trust status of hosts depending on the expiry of saml");
        PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
        AhHostJpaController ahHostJpaController = persistenceServiceFactory.getHostController();
        Reports hostReports = MtwClientFactory
                .getHostReports(mtwPropertiesForverification);

        List<AhHost> ahHostEntities = ahHostJpaController.findAhHostEntities();
        log.info("Fetched {} hosts from attests hub db", ahHostEntities.size());

        for (AhHost ahHost : ahHostEntities) {
            log.info("Processing saml verification for host: {}", ahHost.getId());
            String samlReport = ahHost.getSamlReport();

            TrustAssertion verifyTrustAssertion = convertSamlToTrustAssertion(hostReports, samlReport);
            if (verifyTrustAssertion == null) {
                log.info("No verification report for host: {}", ahHost.getId());
                continue;
            }

            DateTime currentDateTime = new DateTime(DateTimeZone.UTC);
            Date notAfter = verifyTrustAssertion.getNotAfter();
            DateTime notOnOrAfter = new DateTime(notAfter.getTime(), DateTimeZone.UTC);
            log.info("Current Date : {} and saml notOnOrAfter : {}", currentDateTime, notOnOrAfter);
            log.info("notOnOrAfter.isBeforeNow() = {} and  notOnOrAfter.isEqualNow() = {}", notOnOrAfter.isBeforeNow(),
                    notOnOrAfter.isEqualNow());
            if (notOnOrAfter.isBeforeNow() || notOnOrAfter.isEqualNow()) {
                Date issueDate = verifyTrustAssertion.getDate();
                DateTime issueDateUTC = new DateTime(issueDate.getTime(), DateTimeZone.UTC);

                log.info("Marking host : {} as untrusted as the saml issue date is {} and expiring now which is {}",
                        ahHost.getId(), issueDateUTC, currentDateTime);

                ahHost.setTrusted(false);
                try {
                    ahHostJpaController.edit(ahHost);
                } catch (NonexistentEntityException e) {
                    log.error("Unable to update the host as host with id: {} does not exist in the DB ", ahHost.getId(),
                            e);
                } catch (Exception e) {
                    log.error("Unable to update the host with id: {}", ahHost.getId(), e);
                }
            }
        }
        log.info("Update of trust status of hosts depending on the expiry of saml completed");
    }

    private TrustAssertion convertSamlToTrustAssertion(Reports reportsClient, String saml)
            throws AttestationHubException {
        TrustAssertion trustAssertion = null;
        try {
            trustAssertion = reportsClient.verifyTrustAssertion(saml);
        } catch (KeyManagementException e) {
            log.error("KeyManagementException: Error verifying saml", e);
        } catch (CertificateEncodingException e) {
            log.error("CertificateEncodingException: Error verifying saml", e);
        } catch (KeyStoreException e) {
            log.error("KeyStoreException: Error verifying saml", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("NoSuchAlgorithmException: Error verifying saml", e);
        } catch (UnrecoverableEntryException e) {
            log.error("UnrecoverableEntryException: Error verifying saml", e);
        } catch (Exception e) {
            log.error("Exception: Error verifying saml", e);
        }
        return trustAssertion;

    }

    private String convertDateToUTCString(Date date) {
        DateTime dt = new DateTime(date.getTime(), DateTimeZone.UTC);
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        return fmt.print(dt);
    }

    private void populateMwHost(Host host, Report hostReport, Map<String, MWHost> hostIdToMwHostMap, String saml)
            throws AttestationHubException {
        Reports hostReportsVerificationService = MtwClientFactory
                .getHostReports(mtwPropertiesForverification);
        hostReport.setSaml(saml);
        TrustAssertion assertion = convertSamlToTrustAssertion(hostReportsVerificationService,
                saml);
        if (assertion == null) {
            log.error("Unable to verify trust assertion for host : {}", host.getId());
            return;
        }
        MWHost mwHost = new MWHost();
        mwHost.setHost(host);
        mwHost.setMwHostReport(hostReport);
        String str = convertDateToUTCString(assertion.getNotAfter());
        mwHost.setSamlValidTo(str);
        mwHost.setTrustAssertion(assertion);
        mwHost.setTrusted(hostReport.getTrustInformation().isOverall());
        hostIdToMwHostMap.put(host.getId().toString(), mwHost);
        log.info("Received attestation with ID: {} for host ID : {} and name : {}", hostReport.getId(),
                host.getId(), host.getHostName());
    }

    private void populateAttestationServiceProperties() throws AttestationHubException {
        String truststore = Folders.configuration() + File.separator + "truststore.p12";

        if (aasBearerToken == null || aasBearerToken.isEmpty()) {
            updateTokenCache();
        }

        mtwProperties.setProperty("mtwilson.api.tls.policy.certificate.keystore.file", truststore);
        mtwProperties.setProperty("mtwilson.api.tls.policy.certificate.keystore.password", TRUSTSTORE_PASSWORD);
        mtwProperties.setProperty(Constants.MTWILSON_API_URL, AttestationHubConfigUtil.get(Constants.MTWILSON_API_URL));
        mtwProperties.setProperty("bearer.token", aasBearerToken);

        // Verification settings
        mtwPropertiesForverification = new Properties(mtwProperties);
        mtwPropertiesForverification.setProperty("mtwilson.api.truststore", truststore);
        mtwPropertiesForverification.setProperty("mtwilson.api.truststore.password", TRUSTSTORE_PASSWORD);
    }

    private void updateTokenCache () throws AttestationHubException{
        try {
            String trustStoreFileName = Folders.configuration() + File.separator + "truststore.p12";
            TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustStoreFileName, TRUSTSTORE_PASSWORD).build();
            TlsConnection tlsConnection = new TlsConnection(new URL(AttestationHubConfigUtil.get(Constants.AAS_API_URL)), tlsPolicy);

            aasBearerToken = new AASTokenFetcher().getAASToken(
                    AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_ADMIN_USERNAME),
                    AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_ADMIN_PASSWORD),
                    tlsConnection);
        } catch (Exception exc) {
            log.error("Cannot fetch token from AAS: ", exc);
            throw new AttestationHubException("Cannot fetch token from AAS: ", exc);
        }
    }
}
