/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.intel.attestationhub.api.HostDetails;
import com.intel.attestationhub.manager.PluginManager;
import com.intel.attestationhub.plugin.kubernetes.Constants.Plugin;
import com.intel.attestationhub.plugin.kubernetes.client.WLSApiUtil;
import com.intel.attestationhub.service.impl.AttestationHubServiceImpl;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import static com.intel.mtwilson.attestationhub.common.Constants.TRUSTSTORE_PASSWORD;

/**
 * @author arijigh
 * 
 *
 */
public class KubernetesClient {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KubernetesClient.class);

	private static final String NODE_STATUS = "status";
	private static final String NODE_METADATA = "metadata";
	private static final String NODE_NAME = "name";
	private static final String NODE_LABELS = "labels";
	private static final String NODE_CONDITIONS = "conditions";
	private static final String NODE_INFO = "nodeInfo";
	private static final String NODE_SYSTEM_UUID = "systemUUID";
	private static final String NODE_LIST = "items";
	private static final String NODE_CONDITION_TYPE = "type";
	private static final String MASTER_NODE_LABEL = "node-role.kubernetes.io/master";

	private static final String INSTANCE_MANIFEST = "instance_manifest";
	private static final String INSTANCE_INFO = "instance_info";
	private static final String HOST_HARDWARE_UUID = "host_hardware_uuid";
	private static final String VM_REPORT_SIGNATURE = "signature";
	private static final String VM_REPORT_BASE64 = "data";


	TenantConfig tenantConfig = TenantConfig.getTenantConfigObj();

	private Map<String, String> bmTrustStatusMap = new HashMap<>();
	private Map<String, String> nodeNameSystemUUIDMap = new HashMap<>();

	protected KubernetesClient() throws AttestationHubException {
		validateUrl(tenantConfig.getPluginApiEndpoint(), Plugin.URL_TYPE);
	}

	/*
	 * It invokes the validateUrl method of ValidationUtil class to validate the
	 * pluginApiEndpoint configuration parameter received from Attestation hub.
	 * 
	 */
	private void validateUrl(String pluginApiEndpoint, String type) throws AttestationHubException {

		ValidationUtil.validateUrl(pluginApiEndpoint, type);

	}

	/**
	 * This method provides the input to build endpoint URL after determining
	 * the type of formatted CRD object received
	 *
	 * @param jsonList
	 *            IP address of the Kubernetes Master machine, including the
	 *            port number on which of Kubernetes cluster is running.
	 * 
	 */
	public void sendDataToEndpoint(List<String> jsonList) throws AttestationHubException {
		for (String json : jsonList) {
			if (!json.equals(Plugin.NULL)) {
				JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
				String tenantId = jsonObject.getAsJsonObject(Plugin.METADATA).get(Plugin.NAME).toString()
						.replace(Plugin.SLASH_COMMA, Plugin.EMPTY_STRING);
				String urlKind = Plugin.URL_HOSTATTRIBUTES;
				// Create an if block for new CRD
				// To build an URI invoke buildEndpointUri method of this class
				URI uri = buildEndpointUri(jsonObject, tenantId, urlKind);
				new KubernetesCRDUtil().publishCrdToK8s(uri, json);
			}
		}
	}

	/*
	 * It calls into the K8S cluster to get the list of worker nodes available.
	 *
	 */
	protected JsonObject getWorkerNodeDetails() throws AttestationHubException {
		CloseableHttpClient httpClient = new KubernetesCertificateAuthenticator().getHttpClient();
		URI uri = buildWorkerNodeEndpointUri();
		HttpResponse response = getNodeDetails(uri, httpClient);
		HttpEntity entity = response.getEntity();
		JsonObject jsonObject;

		try {
			jsonObject = new JsonParser().parse(EntityUtils.toString(entity)).getAsJsonObject();
		} catch (JsonSyntaxException | IOException e) {
			log.error("Error: " + uri + "Worker node details cannot be accessed");
			throw new AttestationHubException("Error: " + uri + "Worker node details cannot be accessed");
		}
		log.info("Worker node details fetched from K8S master");
		log.debug("Worker node details : {}", jsonObject.toString());
		return jsonObject;
	}

	/*
	 * Performs the HTTP GET operation
	 *
	 * @param uri
	 *            URI to be hit
	 * @param httpClient
	 *            CloseableHttpClient for HTTP operations
	 * @return HTTP response received
	 */
	private HttpResponse getNodeDetails(URI uri, CloseableHttpClient httpClient)
			throws AttestationHubException {
		return new KubernetesConnector().get(httpClient, uri);
	}

	/*
	 * Builds the endpoint URI according to the type of CRD input received.
	 *
	 * When some issue occurs while building the URI. Output: { "Logged error
	 * message" : "Error: Failed building endpoint URI <uri-val>"
	 *
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Failed building endpoint URI
	 *                 <uri-val>" }
	 * @param json
	 *            JsonObject of the CRD type
	 * @param tenantId
	 *            Tenant Id for which the CRD object to be created
	 * @param kind
	 *            Type of CRD for which the endpoint URI has to be build
	 *
	 * @return URI after building, that is, this URI will be called for CRD
	 *         object operations
	 */
	private URI buildEndpointUri(JsonObject json, String tenantId, String kind) throws AttestationHubException {
		String urlString = tenantConfig.getPluginApiEndpoint() + Plugin.PATH + kind + Plugin.SLASH + tenantId;
		URI uri;
		try {
			uri = new URI(urlString);
		} catch (URISyntaxException e) {
			log.error("Error: Failed building endpoint URI", e);
			throw new AttestationHubException("Error: Failed building endpoint URI", e);
		}
		return uri;
	}

	/**
	 * Builds the worker node endpoint URI to get the worker node details .
	 *
	 * When some issue occurs while building the URI. Output: { "Logged error
	 * message" : "Error: Failed building endpoint URI <uri-val>"
	 *
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Failed building worker node endpoint URI
	 *                 <uri-val>" }
	 *
	 * @return URI after building, that is, this URI will be called for CRD
	 *         object operations
	 */
	public URI buildWorkerNodeEndpointUri() throws AttestationHubException {
		String urlString = tenantConfig.getPluginApiEndpoint() + Plugin.NODE_DETAILS_PATH;
		URI uri;
		try {
			uri = new URI(urlString);
		} catch (URISyntaxException e) {
			log.error("Error: Failed building worker node endpoint URI", e);
			throw new AttestationHubException("Error: Failed building worker node endpoint URI", e);
		}
		return uri;
	}

	/**
	 * This method iterates through each CRD object received and call functions
	 * which maps baremetal CRD data to retrieved VMs. After mapping it sends
	 * back a list of string containing CRD objects having baremetal as well as
	 * VMs.
	 *
	 * When any exception comes in the functions invoked in this method Output:
	 * { "Logged error message" : "Error in building VM data"
	 *
	 * @exception: AttestationHubException
	 *                 with the message, "Error in building VM data" }
	 *
	 * @param crdList
	 *            contains CRD objects of platform and geolocation CRD.
	 * @param hostDetails
	 *            contains the host details which was received from attestation
	 *            hub.
	 * @param workerNodeDetails
	 * 			  contains worker node details received from Kubernetes master.
	 * @return a list of CRD objects which contains baremetal as well as VMs
	 *         information.
	 *
	 */
	protected List<String> buildVMData(List<String> crdList, List<HostDetails> hostDetails, JsonObject workerNodeDetails)
			throws AttestationHubException {
		try {
			log.info("CRD list : {}", crdList);
			log.info("Host details list : {}", hostDetails);

			JsonParser parser = new JsonParser();
			JsonArray workerNodeDetailsArray = workerNodeDetails.getAsJsonArray(NODE_LIST);
			Iterator<JsonElement> jsonIterator = workerNodeDetailsArray.iterator();
			Map<String, Map<String, JsonElement>> bmVmTrustMap = getVmTrustMap(jsonIterator);
			Map<String, JsonElement> assetTagMap = getAssetTags(hostDetails);
			List<String> finalList = new ArrayList<>();
			for (String eachCRD : crdList) {
				if (!eachCRD.equals(Plugin.NULL)) {
					JsonArray bmArray = parser.parse(eachCRD).getAsJsonObject().getAsJsonObject(Plugin.SPEC)
							.get(Plugin.HOSTLIST).getAsJsonArray();
					// VM array returned from mapVm method is added to existing
					// baremetal array
					bmArray.addAll(createCrdArray(bmVmTrustMap, bmArray, assetTagMap));
					JsonObject jsonObject = parser.parse(eachCRD).getAsJsonObject();
					// Replacing baremetal array with array of baremetal and VMs
					jsonObject.getAsJsonObject(Plugin.SPEC).add(Plugin.HOSTLIST, bmArray);
					finalList.add(jsonObject.toString());
				}
			}
			return finalList;
		} catch (Exception e) {
			log.error("Error: Error in building VM data", e);
			throw new AttestationHubException("Error: Error in building VM data", e);
		}
	}

	/*
	 * This method is used to check if a particular worker node is ready for container deployment
	 * @param workerNodeDetail
	 * @return boolean
	 */
	private boolean isWorkerNodeReady(JsonObject workerNodeDetail) {
		JsonArray workerNodeConditionArray = workerNodeDetail.getAsJsonObject(NODE_STATUS).getAsJsonArray(NODE_CONDITIONS);
		Iterator<JsonElement> jsonIterator = workerNodeConditionArray.iterator();
		//Adding a check for master node, CRD push skipped in this case
		if (workerNodeDetail.get(NODE_METADATA).getAsJsonObject().get(NODE_LABELS).getAsJsonObject()
				.has(MASTER_NODE_LABEL)) {
			return false;
		}
		while (jsonIterator.hasNext()) {
			JsonObject workerNodeCondition = jsonIterator.next().getAsJsonObject();
			if (workerNodeCondition.get(NODE_CONDITION_TYPE).toString().equals("\"Ready\"") &&
					workerNodeCondition.get(NODE_STATUS).toString().equals("\"True\"")) {
				log.debug("isNodeReady(): Is Worker node {} ready ? {}", workerNodeDetail.get(NODE_METADATA).getAsJsonObject()
						.get(NODE_NAME).toString(),workerNodeCondition.get(NODE_STATUS).toString());
				return true;
			}
		}
		return false;
	}


	/*
	 * This method is used to create a CRD array to be pushed to Kubernetes master. It iterates through the BM and VM
	 * trust status and accordingly creates all the CRDs for each of the worker nodes returns the array.
	 * @param bmVmTrustMap
	 * @param bmArray
	 * @param assetTagMap
	 * @return list of CRDs
	 * @throws AttestationHubException
	 */
	private JsonArray createCrdArray(Map<String, Map<String, JsonElement>> bmVmTrustMap, JsonArray bmArray,
									 Map<String, JsonElement> assetTagMap) throws AttestationHubException{
		JsonParser parser = new JsonParser();
		Gson gson = new Gson();
		JsonArray vmArray = new JsonArray();
		for (JsonElement eachHost : bmArray) {
			JsonObject hostJsonObj = eachHost.getAsJsonObject();
			String bareMetalName = hostJsonObj.get(Plugin.HOSTNAME).toString().replace(Plugin.SLASH_COMMA,
					Plugin.EMPTY_STRING);

			if (bmVmTrustMap.get(bareMetalName) != null && bmVmTrustMap.get(bareMetalName).keySet().size() > 0)
				for (String vmInstanceID : bmVmTrustMap.get(bareMetalName).keySet()) {
					if (bmTrustStatusMap.containsKey(bareMetalName)) {
						Boolean vmTrust = Boolean.valueOf(bmTrustStatusMap.get(bareMetalName)) &&
								validateVMTrustStatus(bmVmTrustMap.get(bareMetalName).get(vmInstanceID).getAsJsonObject());

						String hostCopy = hostJsonObj.toString();
						JsonObject vmJsonObj = parser.parse(hostCopy).getAsJsonObject();
						JsonObject reportJsonObj = getEncryptionData(bareMetalName, parser.parse(vmTrust.toString()),
								parser.parse(vmInstanceID), vmJsonObj.get(Plugin.VALID_TO), assetTagMap);
						vmJsonObj.add(Plugin.TRUSTED, parser.parse(vmTrust.toString()));
						vmJsonObj.add(Plugin.HOSTNAME, parser.parse(nodeNameSystemUUIDMap.get(vmInstanceID)));
						JsonReader reader = new JsonReader(new StringReader(getSignedTrustReport(reportJsonObj).trim()));
						//Done for signed trust report only since it contains characters not to be used in JSON
						reader.setLenient(true);
						//TODO Find a better way to add signed trust report as JSON object
						vmJsonObj.add(Plugin.SIGNED_TRUST_REPORT, parser.parse(gson.toJson(reader)).getAsJsonObject()
								.get("in").getAsJsonObject().get("str"));
						vmArray.add(vmJsonObj);
					}
				}
		}
		return vmArray;
	}


	/*
	 * This method maps baremetal and its asset tags.
	 *
	 * @param hostDetails
	 *            details of hosts received from attestation hub
	 *
	 * @return a map of baremetal and asset tags.
	 *
	 */
	private Map<String, JsonElement> getAssetTags(List<HostDetails> hostDetails) {
		Map<String, JsonElement> assetTagMap = new HashMap<>();
		// Iterating each host and getting its asset tags
		for (HostDetails host : hostDetails) {
			JsonObject jsonObject = new JsonParser().parse(host.trust_report).getAsJsonObject();
			JsonElement hostName = jsonObject.get(Constants.Report.HOSTNAME);
			JsonElement assetTags = jsonObject.get(Constants.Report.ASSET_TAGS);
			assetTagMap.put(hostName.getAsString(), assetTags);
		}
		return assetTagMap;
	}

	/*
	 * This method creates a VM trust map containing VM report linked to the host it is running on and a baremetal trust
	 * map that contains the BM trust status
	 * @param jsonIterator
	 * @return
	 * @throws AttestationHubException
	 */
	private Map<String, Map<String, JsonElement>> getVmTrustMap(Iterator<JsonElement> jsonIterator) throws AttestationHubException {
		JsonArray vmReportArray;
		JsonParser parser = new JsonParser();

		Map<String, JsonElement> vmReportMap = new HashMap<>();
		Map<String, Map<String, JsonElement>> bmVmTrustMap= new HashMap<>();
		AhHost hostInfo;
		String hostName;

		while(jsonIterator.hasNext()) {
			JsonObject workerDetail = jsonIterator.next().getAsJsonObject();

			if (!isWorkerNodeReady(workerDetail)) {
				log.debug("Worker node {} not ready or is master node. Skipping VM report pull...",
						workerDetail.get(NODE_METADATA).getAsJsonObject().get(NODE_NAME).toString());
				continue;
			}
			UUID nodeSystemUUID = UUID.fromString(workerDetail.getAsJsonObject(NODE_STATUS).getAsJsonObject(NODE_INFO)
					.get(NODE_SYSTEM_UUID).toString().replace("\"", ""));
			nodeNameSystemUUIDMap.put(nodeSystemUUID.toString(),
					workerDetail.get(NODE_METADATA).getAsJsonObject().get(NODE_NAME).toString());
			log.info("Node UUID : {}", nodeSystemUUID.toString());

			String vmReportString = new WLSApiUtil().getVmReportByVmID(nodeSystemUUID.toString());
			try {
				vmReportArray = parser.parse(vmReportString).getAsJsonArray();
			} catch(Exception exc) {
				log.debug("No/Invalid VM report received : {}", vmReportString);
				continue;
			}
			if (vmReportArray != null && vmReportArray.size() > 0) {
				String hostHardwareUUID = vmReportArray.get(0).getAsJsonObject().getAsJsonObject(INSTANCE_MANIFEST).
						getAsJsonObject(INSTANCE_INFO).get(HOST_HARDWARE_UUID).
						toString().replace("\"", "");
				if (new AttestationHubServiceImpl().findHostsByHardwareUuid(hostHardwareUUID).size() > 0) {
					hostInfo = new AttestationHubServiceImpl().findHostsByHardwareUuid(hostHardwareUUID).get(0);

					if (hostInfo != null && !hostInfo.getHostName().isEmpty())
						hostName = hostInfo.getHostName();
					else
						continue;
					if (bmTrustStatusMap.get(hostName) == null || bmTrustStatusMap.get(hostName).isEmpty()) {
						boolean hostTrustStatus = hostInfo.getTrusted();
						bmTrustStatusMap.put(hostName, Boolean.toString(hostTrustStatus));
					}

					if (!bmVmTrustMap.containsKey(hostName)) {
						bmVmTrustMap.put(hostName, new HashMap<String, JsonElement>());
					}
					vmReportMap.put(nodeSystemUUID.toString(), vmReportArray.get(0));
					bmVmTrustMap.put(hostName, vmReportMap);
				}
			}
		}
		return bmVmTrustMap;
	}

	/*
	 * This method validates the VM trust status by validating the signature of the VM report and checking the VM
	 * trust status in the report
	 * @param vmReport
	 * @return VM trust status
	 */
	private boolean validateVMTrustStatus(JsonObject vmReport) {
		try {
			String signingCert = vmReport.get("cert").toString().replace("\"", "")
					.replace("\\n", "")
					.replace("-----BEGIN CERTIFICATE-----", "-----BEGIN CERTIFICATE-----\n")
					.replace("-----END CERTIFICATE-----", "\n-----END CERTIFICATE-----");
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate) cf.generateCertificate(IOUtils.toInputStream(signingCert));

			//Verify if certificate is signed by privacy ca
			cert.verify(getPrivacyCaCert());
			String vmReportSignature = vmReport.get(VM_REPORT_SIGNATURE).toString().replace("\"", "");
			String vmReportBase64Encoded = vmReport.get(VM_REPORT_BASE64).toString().replace("\"", "");

			//verify if signature is valid
			Signature sign = Signature.getInstance("SHA256withRSA");
			sign.initVerify(cert);
			sign.update(vmReportBase64Encoded.getBytes());
			sign.verify(Base64.getDecoder().decode(vmReportSignature.getBytes()));
		} catch(CertificateException exc){
			log.debug("Certificate in VM report is not valid : {}", exc.getMessage());
			return false;
		}
		catch (NoSuchAlgorithmException|SignatureException|InvalidKeyException  exc) {
			log.debug("Error verifying signature of VM report {}", exc.getMessage());
			return false;
		}
		catch(Exception exc) {
			log.debug("Error getting privacy ca certificate from IH keystore : {}", exc.getMessage());
		}
		return vmReport.get("trusted").getAsBoolean();
	}

	/*
	 * Gets the privacy CA certificate from IH truststore
	 * @return Privacyca certificate
	 * @throws Exception
	 */
	private PublicKey getPrivacyCaCert() throws Exception{
		final String trustStorePath = Folders.configuration()+"/truststore.";
		String extension = "p12";
		if (KeyStore.getDefaultType().equalsIgnoreCase("JKS")) {
			extension = "jks";
		}
		String trustStoreFileName = trustStorePath + extension;
		KeyStore trustStore = loadTrustStore(trustStoreFileName);
		return trustStore.getCertificate("cn=mtwilson-pca-aik(privacyca)").getPublicKey();
	}

	private KeyStore loadTrustStore(String trustStoreFileName) throws Exception{
		FileInputStream keystoreFIS = new FileInputStream(new File(trustStoreFileName));
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

	/*
	 * This method consolidates all the data to be encrypted
	 * @param bareMetalName
	 * @param vmTrust
	 * @param vmUUID
	 * @param validTo
	 * @param assetTagMap
	 * @return json object to be encrypted
	 */
	private JsonObject getEncryptionData(String bareMetalName, JsonElement vmTrust, JsonElement vmUUID
			, JsonElement validTo, Map<String, JsonElement> assetTagMap) {
		JsonObject reportJsonObj = new JsonObject();
		reportJsonObj.add(Constants.Report.HOSTNAME, vmUUID);
		reportJsonObj.add(Constants.Report.ASSET_TAGS, assetTagMap.get(bareMetalName));
		reportJsonObj.add(Constants.Report.VALID_TO, validTo);
		reportJsonObj.add(Plugin.TRUSTED, vmTrust);
		return reportJsonObj;
	}

	/*
	 * Signs the json object that is added to CRD as signed trust report
	 * @param jsonObj
	 * @return signature of encrypted data
	 * @throws AttestationHubException
	 */
	private String getSignedTrustReport(JsonObject jsonObj) throws AttestationHubException {
		try {
			PluginManager pluginManager = new PluginManager();
			Method method = PluginManager.class.getDeclaredMethod("createSignedTrustReport", String.class);
			method.setAccessible(true);
			String signedTrustReport = method.invoke(pluginManager, jsonObj.toString()).toString();
			return signedTrustReport;
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			log.error("Error: Encoding of trust report failed with ", e);
			throw new AttestationHubException("Error: Encoding of trust report failed with ", e);
		}
	}

}
