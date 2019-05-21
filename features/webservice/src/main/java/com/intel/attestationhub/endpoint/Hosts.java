/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

/**
 * 
 */
package com.intel.attestationhub.endpoint;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;

import com.intel.attestationhub.api.ErrorCode;
import com.intel.attestationhub.api.ErrorResponse;
import com.intel.attestationhub.api.HostFilterCriteria;
import com.intel.attestationhub.service.AttestationHubService;
import com.intel.attestationhub.service.impl.AttestationHubServiceImpl;
import com.intel.dcsg.cpg.validation.RegexPatterns;
import com.intel.dcsg.cpg.validation.ValidationUtil;
import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.launcher.ws.ext.V2;

@V2
@Path("/hosts")
public class Hosts {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Hosts.class);

    @GET
    @Path("/{id:[0-9a-zA-z_-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHost(@PathParam("id") String id) {
	// Validate the ID
	if (!ValidationUtil.isValidWithRegex(id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_ID);
	    errorResponse.detailErrors = "Host Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	AhHost host;
	try {
	    // Fetch the host
	    host = attestationHubService.getHostById(id);
	} catch (AttestationHubException e) {
	    log.error("Error in getting host info");
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    // This error code is if the service throws a non existent entity
	    // exception
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(host).build();
    }

    public Response getHosts() {
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	List<AhHost> hosts = null;
	try {
	    hosts = attestationHubService.getHosts();
	} catch (AttestationHubException e) {
	    log.error("Error in getting all hosts info");
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(hosts).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchHosts(@BeanParam HostFilterCriteria hostFilterCriteria,
	    @Context HttpServletRequest httpServletRequest) {
	log.info("searching for hosts with name : {}", hostFilterCriteria.nameEqualTo);
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();	
	if (StringUtils.isBlank(httpServletRequest.getQueryString())) {
	    return getHosts();
	}
	
	String validate = hostFilterCriteria.validate();
	if (StringUtils.isNotBlank(validate)) {
	    log.error("Invalid Filter criteria for host {}", hostFilterCriteria.nameEqualTo);
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.VALIDATION_FAILED);
	    errorResponse.detailErrors = validate;
	    Status status = Response.Status.BAD_REQUEST;
	    return Response.status(status).entity(errorResponse).build();

	}
	List<AhHost> ahHosts = null;
	try {
	    ahHosts = attestationHubService.searchHostsWithSearchCriteria(hostFilterCriteria);
	} catch (AttestationHubException e) {
	    log.error("Error in searching host with given filter criteria");
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
	}
	return Response.ok(ahHosts).build();
    }
}
