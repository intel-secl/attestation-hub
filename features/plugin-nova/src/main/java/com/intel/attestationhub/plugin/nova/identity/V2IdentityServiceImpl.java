/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.identity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import org.apache.http.HttpResponse;

public class V2IdentityServiceImpl extends AbstractIdentityService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(V2IdentityServiceImpl.class);
    
    public String getAuthRequestBody(String tenantName, String userName, String password, String domainName)
	    throws AttestationHubException {
	validateParams(tenantName, userName, password);
	AuthTokenBody authTokenBody = new AuthTokenBody();
	authTokenBody.auth = new Auth();
	authTokenBody.auth.tenantName = tenantName;
	authTokenBody.auth.passwordCredentials = new PasswordCredentials();
	authTokenBody.auth.passwordCredentials.username = userName;
	authTokenBody.auth.passwordCredentials.password = password;
        
	ObjectMapper mapper = new ObjectMapper();
	String body;
	try {
	    body = mapper.writeValueAsString(authTokenBody);
	} catch (JsonProcessingException e2) {
	    log.error("Error while creating auth token", e2);
	    throw new AttestationHubException("Error while creating auth token", e2);
	}
	return body;
    }
    
    public String getAuthTokenFromResponse(HttpResponse httpResponse) throws AttestationHubException {
	String authToken = null;
        JsonParser parser = new JsonParser();
	boolean responseHasError = false;
	JsonObject obj = parser.parse(responseStr).getAsJsonObject();
	if (obj.has("access")) {
	    JsonObject jsonObjectAccess = obj.getAsJsonObject("access");
	    if (jsonObjectAccess.has("token")) {
		JsonObject property = jsonObjectAccess.getAsJsonObject("token");
		authToken = property.getAsJsonPrimitive("id").getAsString();
	    } else {
		responseHasError = true;
	    }
	} else {
	    responseHasError = true;
	}
        
	if (responseHasError) {
	    log.error("Error fetching authToken from response:{} ", responseStr);
	    throw new AttestationHubException("Error fetching authToken from response");
	}
	return authToken;
    }
    
    @Override
    public String getEndpointUrl(EndpointType type) throws AttestationHubException {
	String endpointUrl = null;
        JsonParser parser = new JsonParser();
	JsonObject obj = parser.parse(responseStr).getAsJsonObject();
	JsonObject access = obj.getAsJsonObject("access");
	JsonArray serviceCatalogArray = access.getAsJsonArray("serviceCatalog");
        
	for (int i = 0; i < serviceCatalogArray.size(); i++) {
	    JsonObject serviceCatalog = (JsonObject) serviceCatalogArray.get(i);
	    if (serviceCatalog.has("endpoints")) {		
		String sc_type = serviceCatalog.getAsJsonPrimitive("type").getAsString();
		if(!"COMPUTE".equalsIgnoreCase(sc_type)){
		    continue;
		}
		JsonArray endpointArray = serviceCatalog.getAsJsonArray("endpoints");
		if(endpointArray.size() > 0){
		    JsonObject endpoint = (JsonObject) endpointArray.get(0);
		    String publicUrl = endpoint.getAsJsonPrimitive("publicURL").getAsString();
		    endpointUrl = publicUrl;
		    break;		    
		}
	    }
	}
	return endpointUrl;
    }
    
    public String getAuthEndpoint(String glanceKeystonePublicEndpoint) {
	return glanceKeystonePublicEndpoint + "/v2.0/tokens";
    }
    
    @JsonInclude(value = Include.NON_NULL)
    class AuthTokenBody {
	public Auth auth;
    }
    
    @JsonInclude(value = Include.NON_NULL)
    class Auth {
	public String tenantName;
	public PasswordCredentials passwordCredentials;
    }
    
    @JsonInclude(value = Include.NON_NULL)
    class PasswordCredentials {
	public String username;
	public String password;
    }
}
