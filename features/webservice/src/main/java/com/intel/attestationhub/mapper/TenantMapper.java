/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.intel.attestationhub.api.Tenant;
import com.intel.mtwilson.attestationhub.data.AhTenant;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.shiro.ShiroUtil;

public class TenantMapper {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TenantMapper.class);

    public static AhTenant mapApiToJpa(Tenant tenant) throws AttestationHubException {
	AhTenant ahTenant = new AhTenant();

	if (StringUtils.isNotBlank(tenant.getId())) {
	    ahTenant.setId(tenant.getId());
	    ahTenant.setModifiedBy(ShiroUtil.subjectUsername());
	} else {
	    ahTenant.setCreatedBy(ShiroUtil.subjectUsername());
	}
	ahTenant.setTenantName(tenant.getName());
	ahTenant.setModifiedBy(ShiroUtil.subjectUsername());
	ahTenant.setModifiedDate(new Date());
	ObjectMapper mapper = new ObjectMapper();
	mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	
	try {
	    String tenantConfig = mapper.writeValueAsString(tenant);
	    ahTenant.setConfig(tenantConfig);
	} catch (JsonGenerationException e) {
	    log.error("Error generating tenant json", e);
	    throw new AttestationHubException(e);
	} catch (JsonMappingException e) {
	    log.error("Error mapping tenant json", e);
	    throw new AttestationHubException(e);
	} catch (IOException e) {
	    log.error("Error creating tenant json", e);
	    throw new AttestationHubException(e);
	}

	return ahTenant;
    }

    public static Tenant mapJpatoApi(AhTenant ahTenant) throws AttestationHubException {
	Tenant tenant = null;
	ObjectMapper mapper = new ObjectMapper();
	mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

	try {
	    String tenantConfig = ahTenant.getConfig();
	    tenant = mapper.readValue(tenantConfig, Tenant.class);
	    tenant.setId(ahTenant.getId());
	    if(ahTenant.getDeleted() == null){
		tenant.setDeleted(false);
	    }else{
		tenant.setDeleted(ahTenant.getDeleted());
	    }
	} catch (JsonGenerationException e) {
	    log.error("Error generating tenant json", e);
	    throw new AttestationHubException(e);
	} catch (JsonMappingException e) {
	    log.error("Error mapping tenant json", e);
	    throw new AttestationHubException(e);
	} catch (IOException e) {
	    log.error("Error creating tenant json", e);
	    throw new AttestationHubException(e);
	}

	return tenant;
    }
}
