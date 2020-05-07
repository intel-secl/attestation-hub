/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

/**
 * This class manage all plugins configured with attestation hub.
 *  It calls all attestation plugins configured for a tenent with 
 *  attestation info of valid hosts
 */
package com.intel.attestationhub.manager;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import com.intel.attestationhub.mapper.TenantMapper;
import com.intel.dcsg.cpg.crypto.Sha1Digest;
import com.intel.mtwilson.attestationhub.controller.AhTenantPluginCredentialJpaController;
import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.data.AhTenantPluginCredential;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.attestationhub.api.HostDetails;
import com.intel.attestationhub.api.HostTrustResponse;
import com.intel.attestationhub.api.PublishData;
import com.intel.attestationhub.api.Tenant;
import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.attestationhub.plugin.EndpointPlugin;
import com.intel.attestationhub.plugin.EndpointPluginFactory;
import com.intel.attestationhub.service.AttestationHubService;
import com.intel.attestationhub.service.impl.AttestationHubServiceImpl;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.controller.AhTenantJpaController;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.attestationhub.data.AhMapping;
import com.intel.mtwilson.attestationhub.data.AhTenant;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.attestationhub.service.PersistenceServiceFactory;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

/**
 * @author Vijay Prakash
 */
public class PluginManager {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginManager.class);
    private static final String PRIVATE_KEY_PATH = Folders.configuration() + File.separator
	    + Constants.PRIVATE_KEY_FILE;
    private static final String PUBLIC_KEY_PATH = Folders.configuration() + File.separator
		+ Constants.PUBLIC_KEY_FILE;

    private static PluginManager pluginManager = null;

    public static PluginManager getInstance() {
	if (pluginManager == null) {
	    pluginManager = new PluginManager();
	}

	return pluginManager;
    }

    public void synchAttestationInfo() {
	log.info("Calling out plugins to push host data");

	List<AhTenant> ahTenantList = retrievAllTenants();
	if (ahTenantList == null) {
	    return;
	}
	log.info("Fetched {} tenants", ahTenantList.size());

	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	for (AhTenant ahTenant : ahTenantList) {
	    Tenant readTenantConfig;
	    try {
		readTenantConfig = TenantMapper.mapJpatoApi(ahTenant);
		log.info("Retrieved configuration for the tenant: {}", ahTenant.getId());
	    } catch (AttestationHubException e) {
		log.error("Error reading configuration for the tenant {}", ahTenant.getId(), e);
		continue;
	    }

	    List<Plugin> plugins = readTenantConfig.getPlugins();
	    Collection<AhMapping> ahMappingCollection = ahTenant.getAhMappingCollection();
	    List<HostDetails> hostsData = new ArrayList<HostDetails>();
	    for (AhMapping ahMapping : ahMappingCollection) {
		if (ahMapping.getDeleted() != null && ahMapping.getDeleted()) {
		    log.info("Mapping {} is not active. Skipping. ", ahMapping.getId());
		    continue;
		}

		String hostHardwareUuid = ahMapping.getHostHardwareUuid();
		AhHost host;
		try {
		    host = attestationHubService.findActiveHostByHardwareUuid(hostHardwareUuid);
		} catch (AttestationHubException e) {
		    log.error("Unable to find an active host with hardware id={}", hostHardwareUuid, e);
		    continue;
		}
		HostDetails details = populateHostDetails(host);
		if (details != null) {
		    log.debug("Adding host details of host uuid: {} to the data published to the controller",
			    host.getId());
		    hostsData.add(details);
		} else {
		    log.error("Populate host details for host uuid: {} returned NULL", host.getId());
		}
	    }
	    if (hostsData.size() == 0) {
		log.info("No host data available for tenant: {}", ahTenant.getId());
		continue;
	    }
	    log.info("Publishing data to the configured plugins for the tenant: {}", ahTenant.getId());
	    processDataToPlugins(ahTenant, hostsData, plugins);
	}
	log.info("Publishing data to plugins complete");
    }

    private List<AhTenant> retrievAllTenants() {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = persistenceServiceFactory.getTenantController();
	List<AhTenant> ahTenantList = tenantController.findAhTenantEntities();
	List<AhTenant> activeTenants = new ArrayList<AhTenant>();

	if (ahTenantList == null) {
	    log.info("No tenants configured");
	    return activeTenants;
	}
	for (AhTenant ahTenant : ahTenantList) {
	    if (ahTenant.getDeleted() != null && ahTenant.getDeleted()) {
		log.info("Tenant {} is not active. Skipping. ", ahTenant.getId());
		continue;
	    }
	    activeTenants.add(ahTenant);
	}
	log.info("Fetched {} tenants", ahTenantList.size());
	return activeTenants;
    }

    private HostDetails populateHostDetails(AhHost host) {
	if (host == null) {
	    return null;
	}
	HostDetails details = new HostDetails();
	String trustTagsJson = host.getTrustTagsJson();
	details.uuid = host.getId();
	details.hardwareUuid = host.getHardwareUuid();
	details.trust_report = trustTagsJson;
	details.hostname = host.getHostName();

	if (StringUtils.isBlank(trustTagsJson)) {
	     log.error("** No trust tags json available for host uuid: {} for generating a JWS", host.getId());
	     return details;
	}

	Map<String, List<String>> assetTags = new HashMap<>();
	Map<String, String> hardwareFeatures = new HashMap<>();
	ObjectMapper objectMapper = new ObjectMapper();

	if (StringUtils.isNotBlank(host.getAssetTags())) {
	    try {
		TypeReference<Map<String, List<String>>> typeRef = new TypeReference<Map<String, List<String>>>() {};
		assetTags = objectMapper.readValue(host.getAssetTags(), typeRef);
	    } catch (JsonParseException e) {
		log.error("Error converting tags to JSON", e);
	    } catch (JsonMappingException e) {
		log.error("Error converting tags to JSON", e);
	    } catch (IOException e) {
		log.error("Error converting tags to JSON", e);
	    }
	}

	if (StringUtils.isNotBlank(host.getHardwareFeatures())) {
	    try {
	        TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
		hardwareFeatures = objectMapper.readValue(host.getHardwareFeatures(), typeRef);
	    } catch (JsonParseException e) {
		log.error("Error converting tags to JSON", e);
	    } catch (JsonMappingException e) {
		log.error("Error converting tags to JSON", e);
	    } catch (IOException e) {
		log.error("Error converting tags to JSON", e);
	    }
	}
	String errorMsg = "Error parsing trust response";
	try {
	    HostTrustResponse hostTrustResponse = objectMapper.readValue(trustTagsJson, HostTrustResponse.class);
	    hostTrustResponse.setValidTo(host.getValidTo());
	    hostTrustResponse.setTrusted(host.getTrusted() == null ? false : host.getTrusted());
	    hostTrustResponse.setAssetTags(assetTags);
	    hostTrustResponse.setHardwareFeatures(hardwareFeatures);
	    String trustReportWithAdditions = objectMapper.writeValueAsString(hostTrustResponse);
	    details.trust_report = trustReportWithAdditions;
	    String signedTrustReport = createSignedTrustReport(trustReportWithAdditions);
	    if (StringUtils.isNotBlank(signedTrustReport)) {
	        details.signed_trust_report = signedTrustReport;
	    }
	} catch (JsonParseException e) {
	    log.error(errorMsg, e);
	} catch (JsonMappingException e) {
	    log.error(errorMsg, e);
	} catch (IOException e) {
	    log.error(errorMsg, e);
	}

	return details;

    }

    private Plugin addCredentialToPlugin(AhTenant ahTenant, Plugin plugin) throws AttestationHubException {
	log.debug("Adding credentials to plugin");
	String tenantId = ahTenant.getId();
    	String pluginName = plugin.getName();
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantPluginCredentialJpaController tenantPluginCredentialController = persistenceServiceFactory.getTenantPluginCredentialController();
	AhTenantPluginCredential ahTenantPluginCredential = tenantPluginCredentialController.findByTenantIdAndPluginName(tenantId, pluginName);
	if (ahTenantPluginCredential == null) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		"Tenant Plugin Credential with tenant id: " + tenantId + " and plugin name: " + pluginName + " does not exist");
	    throw new AttestationHubException(nonexistentEntityException);
	}
	List<Tenant.PluginProperty> properties = ahTenantPluginCredential.getCredential();
	for (Tenant.PluginProperty property : properties) {
	    plugin.addProperty(property.getKey(), property.getValue());
	}

	return plugin;
    }

    private void processDataToPlugins(AhTenant ahTenant, List<HostDetails> hostsData, List<Plugin> plugins) {
	if (plugins == null || hostsData == null || ahTenant == null) {
	    return;
	}
	for (Plugin plugin : plugins) {
	    try {
		PublishData data = new PublishData();
		data.tenantId = ahTenant.getId();
		data.hostDetailsList = hostsData;
		EndpointPlugin endpointPlugin = EndpointPluginFactory.getPluginImpl(plugin);
		if (endpointPlugin == null) {
		    log.info("No plugin available for : {} for tenant with name : {} and id: {}", plugin.getName(), ahTenant.getTenantName(), ahTenant.getId());
		    continue;
		}
		// Adding plugin credentials before pushing data to tenant
		plugin = addCredentialToPlugin(ahTenant, plugin);
		log.info("Before pushing data to plugin : {} of tenant with name : {} and id: {}", plugin.getName(), ahTenant.getTenantName(), ahTenant.getId());
		endpointPlugin.pushData(data, plugin);
		log.info("After pushing data for plugin : {} of tenant with name : {} and id: {}", plugin.getName(), ahTenant.getTenantName(), ahTenant.getId());
	    } catch (AttestationHubException e) {
		log.error("Error pushing data to plugin : {} of tenant with name : {} and id: {}", plugin.getName(), ahTenant.getTenantName(), ahTenant.getId(), e);
		continue;
	    }
	}

    }
    
    private String createSignedTrustReport(String trustReportWithAdditions) {
	PrivateKey privateKey;
	String signedTrustReport = null;
	Map<String, Object> headers = new HashMap<>();
	ObjectMapper objectMapper = new ObjectMapper();
	try {
	    privateKey = loadPrivateKey();
	    headers.put("alg","RS384");
	    headers.put("typ","JWT");
	    headers.put("kid",getKeyId());
	    String jwtHeader = objectMapper.writeValueAsString(headers);
	    if (privateKey == null) {
	        log.error("No privateKey for creating signed report");
		return null;
	    }
	    Signature signature = Signature.getInstance("SHA384withRSA");
	    signature.initSign(privateKey);
	    byte[] trustReportAsBytes = trustReportWithAdditions.getBytes();
	    signature.update(trustReportAsBytes);
	    signedTrustReport = Base64.getUrlEncoder().encodeToString(jwtHeader.getBytes())+ "." + Base64.getUrlEncoder().encodeToString(trustReportWithAdditions.getBytes())
				    + "." +Base64.getUrlEncoder().encodeToString(signature.sign());

	}
	catch (AttestationHubException e) {
            log.error("No private key found for encrypting trust report", e);
	}
	catch (Exception exc) {
	    log.error("Error while signing trust report", exc);
	}
	log.info("JWS format of trust report: {}", signedTrustReport);
	return  signedTrustReport;
    }

    private PrivateKey loadPrivateKey() throws AttestationHubException {
	File prikeyFile = new File(PRIVATE_KEY_PATH);
	if (!(prikeyFile.exists())) {
	    throw new AttestationHubException("Private key unavailable for signing the report");
	}
	FileInputStream fis = null;
	try {
	    fis = new FileInputStream(prikeyFile);
	} catch (FileNotFoundException e) {
	    log.error("Unable to locate private key file at {}", prikeyFile.getAbsolutePath(), e);
	    throw new AttestationHubException("Unable to locate private key file at " + PRIVATE_KEY_PATH, e);
	}
	DataInputStream dis = new DataInputStream(fis);
	byte[] keyBytes = new byte[(int) prikeyFile.length()];
	try {
	    dis.readFully(keyBytes);
	} catch (IOException e) {
	    log.error("Unable to read private key file at {}", prikeyFile.getAbsolutePath(), e);
	}
	try {
	    if (fis != null) {
		fis.close();
	    }
	    if (dis != null) {
	 	dis.close();
	    }
	} catch (IOException e) {
	    log.error("Unable to close stream to private key file at {}", prikeyFile.getAbsolutePath(), e);
	}

	PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
	KeyFactory kf = null;
	try {
	    kf = KeyFactory.getInstance("RSA");
	} catch (NoSuchAlgorithmException e) {
	    log.error("Error", e);
	    throw new AttestationHubException(e);
	}
	PrivateKey generatePrivate = null;
	try {
	    generatePrivate = kf.generatePrivate(spec);
	} catch (InvalidKeySpecException e) {
	    log.error("Error", e);
	    throw new AttestationHubException(e);
	}
	return generatePrivate;
    }

    private String getKeyId() throws AttestationHubException, IOException, CertificateException {
	PublicKey pubkey = loadPublicKey();
	pubkey.getEncoded();
	String keyId = Sha1Digest.digestOf(pubkey.getEncoded()).toBase64();
    	return keyId;
    }

    private PublicKey loadPublicKey() throws AttestationHubException, IOException {
	File pubKeyFile = new File(PUBLIC_KEY_PATH);
	if (!(pubKeyFile.exists())) {
	    throw new AttestationHubException("Private key unavailable for signing the report");
	}
	FileInputStream pubKeyIS = new FileInputStream(PUBLIC_KEY_PATH);
	PemObject pemObject = null;
	PemReader pemReader = new PemReader(new InputStreamReader(pubKeyIS));
	try {
	    pemObject = pemReader.readPemObject();
	} catch (IOException e) {
	    log.error("Error", e);
	} finally {
		pubKeyIS.close();
		pemReader.close();
	}

	byte[] keyBytes;
	if (pemObject == null) {
		log.error("Error reading public key from file: {}", PUBLIC_KEY_PATH);
		throw new AttestationHubException("Error reading public key");
	}
	keyBytes = pemObject.getContent();
	X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(keyBytes);

	KeyFactory kf = null;
	try {
	    kf = KeyFactory.getInstance("RSA");
	} catch (NoSuchAlgorithmException e) {
	    log.error("Error", e);
	    throw new AttestationHubException(e);
	}
	PublicKey generatePublic = null;
	try {
	    generatePublic = (RSAPublicKey) kf.generatePublic(pubKeySpec);
	} catch (InvalidKeySpecException e) {
	    log.error("Error", e);
	    throw new AttestationHubException(e);
	}
	return generatePublic;
    }

}
