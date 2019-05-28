/**
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.attestationhub.plugin.kubernetes.crd;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.intel.attestationhub.api.HostDetails;
import com.intel.attestationhub.plugin.kubernetes.crd.geolocation.GeolocationCRD;
import com.intel.attestationhub.plugin.kubernetes.crd.geolocation.GeolocationMetadata;
import com.intel.attestationhub.plugin.kubernetes.crd.geolocation.GeolocationSpec;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

/**
 * @author abhishekx.negi@intel.com
 *
 * Creates the input for creating a Geolocation CRD object.
 */
public class GeolocationCRDGenerator extends CRDGenerator {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GeolocationCRDGenerator.class);
	
	/**
	 * It parses the mapped host details of a tenant and fetches the variables required for Geolocation CRD object.
	 * After fetching the required variables it put them in the valid CRD object format required to create a Geolocation CRD object. 
	 *
	 * 		When any required CRD parameters are missing in host details data.
	 * 		Output: 
	 * 		{ 
	 * 		"Logged error message" : "Error: Empty trust_report fields- valid_to or hostname or signed_trust_report"
	 * 		@exception: AttestationHubException with the message, "Error: Empty trust_report fields- valid_to or hostname or signed_trust_report"
	 *      }
	 * 		When trusted field is not there in the host's signed report.
	 * 		Output: 
	 * 		{ 
	 * 		"Logged error message" : "Error: Trusted field does not exists"
	 * 		@exception: AttestationHubException with the message, "Error: Trusted field does not exists"
	 * 		}
	 * 
	 * @param details
	 *            Host details mapped to the tenant
	 * @param tenantId
	 *            Tenant Id for which the data is received from the Attestation Hub
	 * @return a formatted input for creating a Geolocation CRD object. 
	 * 
	 */
	@Override
	protected String createCRD(List<HostDetails> details, String tenantId) throws AttestationHubException {
		GeolocationSpec locationSpec = new GeolocationSpec();
		for (HostDetails hostDetails : details) {
			JsonObject jsonObject = new JsonParser().parse(hostDetails.trust_report).getAsJsonObject();
			JsonElement validTo = jsonObject.get(Constants.VALID_TO);
			JsonElement hostName = jsonObject.get(Constants.HOSTNAME);
			String signedReport = hostDetails.signed_trust_report;
			if (validTo == null || hostName == null || signedReport.isEmpty()) {
				log.error("Error: Empty trust_report fields- valid_to or hostname or signed_trust_report");
				throw new AttestationHubException(
						"Error: Empty trust_report fields- valid_to or hostname or signed_trust_report");
			}
			SimpleEntry<Map<String, String>, Boolean> simpleEntry = checkAssetTags(jsonObject, hostName);
			if (simpleEntry != null && simpleEntry.getValue()) {
				locationSpec.createSpecField(validTo, signedReport, hostName, simpleEntry.getKey());
			}
		}

		GeolocationCRD crd = null;
		if (locationSpec.getHostList() != null) {
			GeolocationMetadata metadata = new GeolocationMetadata();
			metadata.setName(tenantId.toLowerCase() + Constants.HYPHEN + Constants.CIT + Constants.HYPHEN
					+ Constants.GEOLOCATION + Constants.HYPHEN + Constants.OBJECT);
			crd = new GeolocationCRD();
			crd.setApiVersion(Constants.API_VERSION);
			crd.setKind(Constants.LOCATION_CRD);
			crd.setMetadata(metadata);
			crd.setSpec(locationSpec);
		}
		return new Gson().toJson(crd);
	}
	
	/**
	 * It validates the asset tags of each host of a tenant.
	 *
	 * 		When an asset tag has more than one value
	 * 		Output: 
	 * 		{ 
	 * 		"Logged error message" : "Error: Multiple values in asset_tag field"
	 *      }
	 * 		When asset tag key has space or any special characters except forward slash, underscore, hyphen or dot
	 * 		Output: 
	 * 		{ 
	 * 		"Logged error message" : "Error: Invalid key: <key-name> in asset_tags- Value should contain only forward 
	 * 									slash /,underscore, hyphen or dot ."
	 * 		}
	 * 		When asset tag value has space or any special characters except forward slash, underscore, hyphen or dot
	 * 		Output: 
	 * 		{ 
	 * 		"Logged error message" : "Error: Invalid value: <value-name> in asset_tags- Value should contain only underscore, hyphen or dot ."
	 * 		}
	 * 
	 * @param jsonObject
	 *            Trust report data in JSON form.
	 * @param hostName
	 *            Hostname value 
	 * @return a SimpleEntry<Map<String, String>, Boolean>, which will have a key as a map of key,value pair of asset tag and value as boolean.
	 * 		   This boolean will decide if the host will be included in the Tenant's Geolocation CRD object or not. 
	 * 			 
	 * 
	 */
	private SimpleEntry<Map<String, String>, Boolean> checkAssetTags(JsonObject jsonObject, JsonElement hostName) {
		JsonElement jsonElement = jsonObject.get(Constants.ASSET_TAGS);
		if (!jsonElement.toString().equals(Constants.EMPTY_ASSET_TAGS)) {
			Map<String, ArrayList<String>> assetMap = new Gson().fromJson(jsonElement.toString(),
					new TypeToken<HashMap<String, ArrayList<String>>>() {
					}.getType());
			Map<String, String> respMap = new HashMap<String, String>();
			for (String key : assetMap.keySet()) {
				// Checking there is only one value
				if (assetMap.get(key).size() != Constants.ONE) {
					log.error("Error: Multiple values in asset_tag field");
					return new SimpleEntry<Map<String, String>, Boolean>(respMap, false);
				}
				// Checking key should not contain space and other special
				// characters except forward slash, underscore, hyphen or dot
				if (!Pattern.matches(Constants.ASSET_LABEL_REGEX, key)) {
					log.error("Error: Invalid key: " + key + "in asset_tags"
							+ "- Value should contain only forward slash /,underscore, hyphen or dot .");
					return new SimpleEntry<Map<String, String>, Boolean>(respMap, false);
				}
				// Checking value should not contain space and other special
				// characters except underscore, hyphen or dot
				String val=assetMap.get(key).get(Constants.ZERO);
				if (!Pattern.matches(Constants.ASSET_VALUE_REGEX,val)) {
					log.error("Error: Invalid value: "+val+" of key: " + key
							+ "- Value should contain only underscore, hyphen or dot .");
					return new SimpleEntry<Map<String, String>, Boolean>(respMap, false);
				}
				respMap.put(key,val);
			}
			return new SimpleEntry<Map<String, String>, Boolean>(respMap, true);
		}
		log.info("Info: Empty asset_tags for " + hostName);
		return null;
	}
}
