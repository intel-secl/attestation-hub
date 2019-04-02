/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.endpoint;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
import com.intel.attestationhub.api.Tenant;
import com.intel.attestationhub.api.TenantFilterCriteria;
import com.intel.attestationhub.service.AttestationHubService;
import com.intel.attestationhub.service.impl.AttestationHubServiceImpl;
import com.intel.dcsg.cpg.validation.RegexPatterns;
import com.intel.dcsg.cpg.validation.ValidationUtil;
import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.launcher.ws.ext.V2;

/**
 * @author GS Lab
 * 
 */
@V2
@Path("/tenants")
public class Tenants {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Tenants.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTenant(Tenant tenant) {
	String validateResult = tenant.validate();
	if (StringUtils.isNotBlank(validateResult)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.VALIDATION_FAILED);
	    errorResponse.detailErrors = validateResult;
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	try {
	    String newTenantId = attestationHubService.createTenant(tenant);
	    tenant.setId(newTenantId);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
	}
	return Response.ok(tenant).build();
    }

    @GET
    @Path("/{id:[0-9a-zA-Z_-]+ }")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveTenant(@PathParam("id") String id) {
	if (!ValidationUtil.isValidWithRegex(id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_ID);
	    errorResponse.detailErrors = "Tenant Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}

	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	Tenant tenant;
	try {
	    tenant = attestationHubService.retrieveTenant(id);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(tenant).build();
    }

    private Response retrieveAllTenants() {
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	List<Tenant> tenants;
	try {
	    tenants = attestationHubService.retrieveAllTenants();
	} catch (AttestationHubException e) {
	    log.error("Error searching for all atenants", e);
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(tenants).build();
    }

    @PUT
    @Path("/{id:[0-9a-zA-Z_-]+ }")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTenant(@PathParam("id") String id, Tenant tenant) {
	if (!ValidationUtil.isValidWithRegex(id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_ID);
	    errorResponse.detailErrors = "Tenant Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}

	String validateResult = tenant.validate();
	if (StringUtils.isNotBlank(validateResult)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.VALIDATION_FAILED);
	    errorResponse.detailErrors = validateResult;
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}

	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	Tenant newTenant;
	try {
	    tenant.setId(id);
	    newTenant = attestationHubService.updateTenant(tenant);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(newTenant).build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:[0-9a-zA-Z_-]+ }")
    public Response deleteTenant(@PathParam("id") String id) {
	if (!ValidationUtil.isValidWithRegex(id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_ID);
	    errorResponse.detailErrors = "Tenant Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}
	Tenant tenant = null;
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	try {
	    tenant = attestationHubService.retrieveTenant(id);
	    attestationHubService.deleteTenant(id);
	    tenant.setDeleted(true);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(tenant).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchTenantsBySearchCriteria(@BeanParam TenantFilterCriteria tenantFilterCriteria,
	    @Context HttpServletRequest httpServletRequest) {
	log.info("Searching for tenants with name : {}", tenantFilterCriteria.nameEqualTo);
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();

	if (StringUtils.isBlank(httpServletRequest.getQueryString())) {
	    return retrieveAllTenants();
	}
	String validate = tenantFilterCriteria.validate();
	if (StringUtils.isNotBlank(validate)) {
	    log.error("Invalid tenant search criteria: {}", validate);
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.VALIDATION_FAILED);
	    errorResponse.detailErrors = validate;
	    Status status = Response.Status.BAD_REQUEST;
	    return Response.status(status).entity(errorResponse).build();

	}
	List<Tenant> tenantsList;
	try {
	    tenantsList = attestationHubService.searchTenantsBySearchCriteria(tenantFilterCriteria);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(tenantsList).build();
    }
}
