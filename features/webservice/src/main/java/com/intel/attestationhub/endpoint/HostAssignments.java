/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.endpoint;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import com.intel.attestationhub.api.Mapping;
import com.intel.attestationhub.api.MappingResultResponse;
import com.intel.attestationhub.api.SearchCriteriaForMapping;
import com.intel.attestationhub.service.AttestationHubService;
import com.intel.attestationhub.service.impl.AttestationHubServiceImpl;
import com.intel.dcsg.cpg.validation.RegexPatterns;
import com.intel.dcsg.cpg.validation.ValidationUtil;
import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.data.AhMapping;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.launcher.ws.ext.V2;

/**
 * @author GS Lab
 * 
 */
@V2
@Path("/")
public class HostAssignments {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HostAssignments.class);

    @POST
    @Path("/host-assignments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMapping(Mapping mapping) {
	if (!ValidationUtil.isValidWithRegex(mapping.tenant_id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_ID);
	    errorResponse.detailErrors = "Tenant Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}

	if (mapping.hardware_uuids == null || mapping.hardware_uuids.size() == 0) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.VALIDATION_FAILED);
	    errorResponse.detailErrors = "Hosts Id information is mandatory";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}
	List<String> invalidhardwareuuids = new ArrayList<>();

	for (String hardware_uuid : mapping.hardware_uuids) {
	    if (!ValidationUtil.isValidWithRegex(hardware_uuid, RegexPatterns.UUID)) {
		invalidhardwareuuids.add(hardware_uuid);
	    }
	}
	if (invalidhardwareuuids.size() > 0) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_ID);
	    errorResponse.detailErrors = "Hardware UUIDS are not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();

	}

	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	MappingResultResponse mappingResultResponse;
	try {
	    mappingResultResponse = attestationHubService.createOrUpdateMapping(mapping.tenant_id,
		    mapping.hardware_uuids);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
	}
	return Response.ok(mappingResultResponse).build();
    }

    @GET
    @Path("host-assignments/{id:[0-9a-zA-Z_-]+ }")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveMapping(@PathParam("id") String id) {
	if (!ValidationUtil.isValidWithRegex(id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_ID);
	    errorResponse.detailErrors = "Mapping Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}

	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	AhMapping ahMapping;
	try {
	    ahMapping = attestationHubService.retrieveMapping(id);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.errorMessage = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(ahMapping).build();
    }

    public Response retrieveAllMappings() {
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	List<AhMapping> ahMappings;
	try {
	    ahMappings = attestationHubService.retrieveAllMappings();
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.errorMessage = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(ahMappings).build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("host-assignments/{id:[0-9a-zA-Z_-]+ }")
    public Response deleteMapping(@PathParam("id") String id) {
	if (!ValidationUtil.isValidWithRegex(id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_ID);
	    errorResponse.detailErrors = "Mapping Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}
	AhMapping ahMapping = null;
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	try {
	    ahMapping = attestationHubService.retrieveMapping(id);
	    attestationHubService.deleteMapping(id);
	    ahMapping.setDeleted(true);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.errorMessage = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(ahMapping).build();
    }

    @GET
    @Path("host-assignments")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchMappingsBySearchCriteria(@BeanParam SearchCriteriaForMapping criteriaForMapping,
	    @Context HttpServletRequest httpServletRequest) {
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	if (StringUtils.isBlank(httpServletRequest.getQueryString())) {
	    return retrieveAllMappings();
	}
	String validate = criteriaForMapping.validate();
	if (StringUtils.isNotBlank(validate)) {
	    log.error("Invalid host assignment search criteria: {}", validate);
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.VALIDATION_FAILED);
	    errorResponse.detailErrors = validate;
	    Status status = Response.Status.BAD_REQUEST;
	    return Response.status(status).entity(errorResponse).build();
	}
	List<AhMapping> ahMappings;

	try {
	    ahMappings = attestationHubService.searchMappingsBySearchCriteria(criteriaForMapping);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.errorMessage = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(ahMappings).build();
    }

}
