/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.MediaType;

import com.intel.dcsg.cpg.tls.policy.TlsConnection;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.tls.policy.TlsPolicyBuilder;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.core.common.utils.AASTokenFetcher;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jclouds.ContextBuilder;
import org.jclouds.openstack.keystone.config.KeystoneProperties;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import com.google.common.io.Closeables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intel.attestationhub.api.HostDetails;
import com.intel.attestationhub.manager.PluginManager;
import com.intel.attestationhub.plugin.kubernetes.Constants.Plugin;
import com.intel.attestationhub.plugin.kubernetes.Constants.Report;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

import static com.intel.mtwilson.attestationhub.common.Constants.TRUSTSTORE_PASSWORD;

/**
 * @author abhishekx.negi@intel.com
 * 
 *         This class create openstack connection, get VMs and map baremetal
 *         data to retrieved VMs. Returns CRD objects containing both baremetal
 *         and VM information
 */

public class OpenstackClient {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OpenstackClient.class);
	// Getting mtwilson configuration from existing class
	TenantConfig tenant = TenantConfig.getTenantConfigObj();
	private NovaApi novaApi;

	private Map<String, String> vmInstanceIdMap = new HashMap<>();
	private Map<String, Boolean> bmTrustMap = new HashMap<>();
	private Map<String, JsonElement> retrievedVmTrust = new HashMap<>();

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
	 * @return a list of CRD objects which contains baremetal as well as VMs
	 *         information.
	 * 
	 */
	protected List<String> buildVMData(List<String> crdList, List<HostDetails> hostDetails)
			throws AttestationHubException {
		try {
			createConnection();
			Map<String, List<String>> bmVmMap = listServers(novaApi.getConfiguredRegions());
			log.info("baremetal vm mapping received from openstack ",bmVmMap);
			closeConnection();
			Map<String, JsonElement> assetTagMap = getAssetTags(hostDetails);
			List<String> finalList = new ArrayList<>();
			JsonParser parser = new JsonParser();
			for (String eachCRD : crdList) {
                                if (!eachCRD.equals(Plugin.NULL)) {
                                        JsonArray bmArray = parser.parse(eachCRD).getAsJsonObject().getAsJsonObject(Plugin.SPEC)
                                                        .get(Plugin.HOSTLIST).getAsJsonArray();
                                        // VM array returned from mapVm method is added to existing
                                        // baremetal array
                                        bmArray.addAll(mapVM(bmVmMap, bmArray, assetTagMap));
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

	/**
	 * This method create Openstack connection by using the configuration passed
	 * during tenant registration.
	 *
	 * When Openstack connection can't be established due to any reason. Output:
	 * { "Logged error message" : "Error: Couldn't create openstack connection.
	 * Check openstack config"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Couldn't create openstack
	 *                 connection. Check openstack config" }
	 * 
	 * 
	 */
	private void createConnection() throws AttestationHubException {
		final Properties properties = new Properties();
		// Putting tenant configuration values to create openstack connection
		properties.put(KeystoneProperties.KEYSTONE_VERSION, tenant.getKeystoneVersion());
		properties.put(KeystoneProperties.TENANT_NAME, tenant.getOpenstackTenantName());
		properties.put(KeystoneProperties.SCOPE, tenant.getOpenstackScope());
		try {
			novaApi = ContextBuilder.newBuilder(Plugin.PROVIDER).endpoint(tenant.getOpenstackURI())
					.credentials(tenant.getOpenstackUsername(), tenant.getOpenstackPass()).overrides(properties)
					.buildApi(NovaApi.class);
		} catch (Exception e) {
			log.error("Error: Couldn't create openstack connection. Check openstack config", e);
			throw new AttestationHubException("Error: Couldn't create openstack connection. Check openstack config", e);
		}
	}

	/**
	 * This method list all the VMs according to regions and map them in a hash
	 * map, in which key is the hostname on which particular VM is running and
	 * key is the list of VMs.
	 *
	 * When any exception comes while retrieving the VM information Output: {
	 * "Logged error message" : "Not able to list VMs, check openstack config"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Not able to list VMs, check openstack
	 *                 config" }
	 * 
	 * @param regions
	 *            A set of regions retrieved after successful openstack
	 *            connection
	 *
	 * @return a hash map which contains hostname as the key and list of VMs as
	 *         value
	 * 
	 */

	private Map<String, List<String>> listServers(Set<String> regions) throws AttestationHubException {
		Map<String, List<String>> bmVmMap = new HashMap<>();
		try {
			// VMs will be running in regions so iterating by regions first
			for (String region : regions) {
				ServerApi serverApi = novaApi.getServerApi(region);
				// getting each server details running in region
				for (Server server : serverApi.listInDetail().concat()) {
					String hostName = server.getExtendedAttributes().get().getHostName();
					List<String> vmList = bmVmMap.get(hostName);
					if (vmList == null) {
						vmList = new ArrayList<>();
						if (hostName != null) {
							bmVmMap.put(hostName, vmList);
						}
					}
					// If VM is in error state then it won't have an IP. In this
					// case we
					// will skip the VM entry.
					String vmIP = null;
					try {
						vmIP = server.getAddresses().entries().iterator().next().getValue().getAddr();
					} catch (Exception e) {
						log.error("Skipping VM " + server.getId()
								+ " because VM IP is not assigned as it is not in active state");
						continue;
					}

					// Adding VM IP and its instance ID to map, serves as an
					// input to mtwilson
					String baremetalVmIp = hostName + Plugin.STRING_HYPHEN + vmIP;
					vmInstanceIdMap.put(baremetalVmIp, server.getId());
					vmList.add(baremetalVmIp);
				}
			}
		} catch (Exception e) {
			log.error("Error: Not able to list VMs, check openstack config", e);
			throw new AttestationHubException("Error: Not able to list VMs, check openstack config", e);
		}
		return bmVmMap;
	}

	/**
	 * Method to close the created openstack connection
	 *
	 * When any exception comes while closing the openstack connection Output: {
	 * "Logged error message" : "Error: Openstack connection closed abruptly"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Openstack connection closed
	 *                 abruptly" }
	 * 
	 */
	private void closeConnection() throws AttestationHubException {
		try {
			Closeables.close(novaApi, true);
		} catch (IOException e) {
			log.error("Error: Openstack connection closed abruptly", e);
			throw new AttestationHubException("Error: Openstack connection closed abruptly", e);
		}
	}

	/**
	 * This method maps respective baremetal data to the VMs after retrieving
	 * the vm trust from mtwilson and encrypting the signed trust report.
	 *
	 * @param bmVmMap
	 *            Map of baremetal and VMs
	 * 
	 * @param bmArray
	 *            A baremetal array which contains CRD spec fields
	 *
	 * @param assetTagMap
	 *            Map which contains key as baremetal name and value as their
	 *            respective tags
	 * 
	 * @return a json array which contains baremetals and VMs spec fields
	 * 
	 */
	private JsonArray mapVM(Map<String, List<String>> bmVmMap, JsonArray bmArray, Map<String, JsonElement> assetTagMap)
			throws AttestationHubException {
		JsonArray vmArray = new JsonArray();
		// Iterating for each baremetal
		for (JsonElement eachHost : bmArray) {
			JsonObject hostJsonObj = eachHost.getAsJsonObject();
			String bareMetal = hostJsonObj.get(Plugin.HOSTNAME).toString().replace(Plugin.SLASH_COMMA,
					Plugin.EMPTY_STRING);
			// Geolocation CRD object won't have trusted field, so taking it
			// from its respective platform CRD
			if (hostJsonObj.has(Plugin.TRUSTED)) {
				bmTrustMap.put(bareMetal, hostJsonObj.get(Plugin.TRUSTED).getAsBoolean());
			}
			if (bmVmMap.containsKey(bareMetal)) {
				JsonParser parser = new JsonParser();
				for (String vm : bmVmMap.get(bareMetal)) {
					JsonElement vmTrust = parser.parse(Plugin.STRING_FALSE);
					String vmInstanceId = vmInstanceIdMap.get(vm);
					// Checking if VM instance ID already exists in map to avoid
					// hitting mtwilson
					if (retrievedVmTrust.containsKey(vmInstanceId)) {
						vmTrust = retrievedVmTrust.get(vmInstanceId);
					} else {
						// VM trust will only be retrieved if its host's trust
						// is true
						if (bmTrustMap.get(bareMetal) == true) {
							vmTrust = getVmTrustStatus(bareMetal, vmInstanceId);
							// Saving VM trust in retrievedVMTrust map to avoid
							// hitting mtwilson for geolocation
							retrievedVmTrust.put(vmInstanceId, vmTrust);
						}
					}
					JsonElement baremetalVmIp = parser.parse(vm);
					// Performing deep copy. Values of baremetal will be
					// overridden if deep copy is not performed
					String hostCopy = hostJsonObj.toString();
					JsonObject vmJsonObj = parser.parse(hostCopy).getAsJsonObject();
					JsonElement validTo = vmJsonObj.get(Plugin.VALID_TO);
					// K8S extended scheduler expects same fields for platform
					// CRD and
					// geolocation CRD so getting accumulated fields for both
					JsonObject reportJsonObj = getEncryptionData(bareMetal, vmTrust, baremetalVmIp, validTo,
							assetTagMap);
					if (hostJsonObj.has(Plugin.TRUSTED)) {
						vmJsonObj.add(Plugin.TRUSTED, vmTrust);
					}
					// Host name is replaced by VM IP
					vmJsonObj.add(Plugin.HOSTNAME, baremetalVmIp);
					vmJsonObj.add(Plugin.SIGNED_TRUST_REPORT, parser.parse(getSignedTrustReport(reportJsonObj)));
					vmArray.add(vmJsonObj);
				}
			}
		}
		return vmArray;
	}

	/**
	 * This method bundle the data required for signed trust report
	 *
	 * @param bareMetal
	 *            baremetal name or IP
	 * 
	 * @param vmTrust
	 *            trust status of VM
	 * 
	 * @param validTo
	 *            Timestamp of signed trust report. For VM it is same as of its
	 *            baremetal
	 *
	 * @param assetTagMap
	 *            Map which contains baremetal as key and respective asset tags
	 *            as value
	 * 
	 * 
	 * @return a json object, which is a bundle of all the arguments provided.
	 * 
	 */

	private JsonObject getEncryptionData(String bareMetal, JsonElement vmTrust, JsonElement baremetalVmIp,
			JsonElement validTo, Map<String, JsonElement> assetTagMap) {
		JsonObject reportJsonObj = new JsonObject();
		reportJsonObj.add(Report.HOSTNAME, baremetalVmIp);
		reportJsonObj.add(Report.ASSET_TAGS, assetTagMap.get(bareMetal));
		reportJsonObj.add(Report.VALID_TO, validTo);
		reportJsonObj.add(Plugin.TRUSTED, vmTrust);
		return reportJsonObj;
	}

	/**
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
			JsonElement hostName = jsonObject.get(Report.HOSTNAME);
			JsonElement assetTags = jsonObject.get(Report.ASSET_TAGS);
			assetTagMap.put(hostName.getAsString(), assetTags);
		}
		return assetTagMap;
	}

	/**
	 * This method is used to get VM trust status from mtwilson. A connection to
	 * mtwilson is created by providing the hostname and VM instance ID for
	 * which retrieval of trust status is required
	 *
	 * When not able to convert payload to HttpEntity Output: { "Logged error
	 * message" : "Unable to convert request into HttpEntity"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Unable to convert request into
	 *                 HttpEntity" }
	 * 
	 *                 When unable to connect to mtwilson Output: { "Logged
	 *                 error message" : "Error: Post method failed with
	 *                 exception"
	 * @exception: AttestationHubException
	 *                 with the message, "Error: Post method failed with
	 *                 exception" }
	 * 
	 * @param hostName
	 *            Host name or IP on which VM is running
	 *
	 * @param vmInstanceId
	 *            VM instance ID for which trust status is to retrieved
	 *
	 * @return a json element which true or false as trust status
	 * 
	 */
	private JsonElement getVmTrustStatus(String hostName, String vmInstanceId) throws AttestationHubException {
		String trustStoreFileName = Folders.configuration() + File.separator + "truststore.p12";
		String aasBearerToken = null;
		try {
			TlsPolicy tlsPolicy = TlsPolicyBuilder.factory().strictWithKeystore(trustStoreFileName, TRUSTSTORE_PASSWORD).build();

			TlsConnection tlsConnection = new TlsConnection(new URL(AttestationHubConfigUtil.get(Constants.AAS_API_URL)), tlsPolicy);
			aasBearerToken = new AASTokenFetcher().getAASToken(
					AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_SERVICE_USERNAME),
					AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_SERVICE_PASSWORD),
					tlsConnection);
		} catch (Exception e) {
			e.printStackTrace();
		}
		HttpPost httppost = new HttpPost(
				AttestationHubConfigUtil.get(Constants.MTWILSON_API_URL) + Plugin.MTWILSON_URI);
		httppost.setHeader("Authorization", "Bearer " + aasBearerToken);
		String json = "{\"host_name\":\"" + hostName + "\",\"vm_instance_id\":\"" + vmInstanceId
				+ "\",\"include_host_report\":false}";
		try {
			httppost.setEntity(new StringEntity(json));
		} catch (UnsupportedEncodingException e) {
			log.error("Error: Unable to convert request into HttpEntity", e);
			throw new AttestationHubException("Error: Unable to convert request into HttpEntity", e);
		}
		httppost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
		httppost.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
		HttpResponse response = null;
		SSLContext sslcontext = getSSLContext();
		SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext,
				SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		CloseableHttpClient client = HttpClients.custom().setSSLSocketFactory(factory).build();
		JsonParser jsonParser = new JsonParser();
		try {
			response = client.execute(httppost);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				String vmReport = new BasicResponseHandler().handleResponse(response);
				log.info("received vm report from mtwilson ", vmReport);
				return jsonParser.parse(vmReport).getAsJsonObject().get(Plugin.TRUST_STATUS);
			} else {
				// Instead of raising an exception, returning VM trust as false
				// if response code 200 is not recieved.
				log.error("Error: Unable to retrieve VM trust status for " + vmInstanceId + ". Reason-",
						response.getStatusLine());
				return jsonParser.parse(Plugin.STRING_FALSE);
			}
		} catch (IOException e) {
			log.error("Error: Post method failed with exception ", e);
			throw new AttestationHubException("Error: Post method failed with exception", e);
		}
	}

	/**
	 * This method validate the existing mtwilson keystore and return the SSL
	 * context which is required to create mtwilson connection
	 *
	 * Any exception comes getting the SSL context Output: { "Logged error
	 * message" : "Method getSSLContext failed with exception"
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Method getSSLContext failed with
	 *                 exception" }
	 *
	 * @return SSL context to create openstack connection
	 * 
	 */

	private SSLContext getSSLContext() throws AttestationHubException {
		try (FileInputStream instream = new FileInputStream(
				new File(Plugin.MTWILSON_CONFIG_DIR + "truststore.p12"))) {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(instream, TRUSTSTORE_PASSWORD.toCharArray());
			return SSLContexts.custom().loadTrustMaterial(trustStore).build();
		} catch (Exception e) {
			log.error("Error: Method getSSLContext failed with exception ", e);
			throw new AttestationHubException("Error: Method getSSLContext failed with exception ", e);
		}
	}

	/**
	 * This method is used to call the existing function to encrypt the report
	 * for VMs.
	 *
	 * If any exception comes while calling the function Output: { "Logged error
	 * message" : "Encoding of trust report failed with "
	 * 
	 * @exception: AttestationHubException
	 *                 with the message, "Encoding of trust report failed with "
	 *                 }
	 *
	 * @param jsonObj
	 *            a json object which contains the spec field of VM
	 *
	 * @return Signed trust report
	 * 
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
