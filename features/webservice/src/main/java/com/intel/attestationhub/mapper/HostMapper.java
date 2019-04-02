/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.mapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.core.xml.XMLObject;
import org.w3c.dom.Element;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.attestationhub.api.MWHost;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.flavor.rest.v2.model.Host;
import com.intel.mtwilson.flavor.rest.v2.model.Report;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.model.Hostname;
import com.intel.mtwilson.supplemental.saml.TrustAssertion;
import com.intel.mtwilson.shiro.ShiroUtil;
import java.security.cert.CertificateException;
import com.intel.attestationhub.api.FlavorStatusResponse;

public class HostMapper {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HostMapper.class);

    public static AhHost mapHostToAhHost(MWHost host, AhHost ahHost, String user) {
	Host citHost = host.getHost();
	Report citHostReport = host.getMwHostReport();
	Date currentdate = new Date();
	String currentUser = user;
	if (StringUtils.isBlank(user)) {
	    currentUser = "admin";
	}

	if (ahHost == null) {
	    ahHost = new AhHost();
	    ahHost.setId(citHost.getId().toString());
	    ahHost.setCreatedBy(currentUser);
	}
	ahHost.setModifiedBy(currentUser);
	ahHost.setModifiedDate(currentdate);
	ahHost.setAikCertificate(null); //citHostReport.getTrustReport().getHostManifest().getAikCertificate().toString()
	ahHost.setAikSha256(null); //Sha256Digest.digestOf(citHostReport.getTrustReport().getHostManifest().getAikCertificate().toString().getBytes()).toString()
	ahHost.setBiosMleUuid(null);
	ahHost.setConnectionUrl(citHost.getConnectionString());
	ahHost.setHardwareUuid(citHost.getHardwareUuid().toString());
	ahHost.setHostName(citHost.getHostName());
	ahHost.setValidTo(host.getSamlValidTo());
	ahHost.setTrusted(host.getTrusted() == null ? false : host.getTrusted());
        Map<String, String> trustTagValueList = new HashMap<>();
	if (host.getTrustAssertion() != null) {
	    TrustAssertion trustAssertion = host.getTrustAssertion();
	    Assertion assertion = trustAssertion.getAssertion();
	    List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
	    String tag;
	    String tagValue;
	    String trustTag;
	    String trustTagValue = null;
	    Map<String, List<String>> assetTagToValueMap = new HashMap<String, List<String>>();
	    List<String> tagValueList;
	    for (AttributeStatement attributeStatement : attributeStatements) {
		List<Attribute> attributes = attributeStatement.getAttributes();
		for (Attribute attribute : attributes) {
		    tagValue = null;
		    String name = attribute.getName();
		    if (name.startsWith(Constants.SAML_TAG)) {
                        tag = name;
			if (StringUtils.isBlank(tag)) {
			    continue;
			}
			if (assetTagToValueMap.containsKey(tag)) {
			    tagValueList = assetTagToValueMap.get(tag);
			} else {
			    tagValueList = new ArrayList<String>();
			    assetTagToValueMap.put(tag, tagValueList);
			}
			List<XMLObject> attributeValues = attribute.getAttributeValues();
			for (XMLObject xmlObject : attributeValues) {
			    Element dom = xmlObject.getDOM();
			    tagValue = dom.getTextContent();
			}
			if (StringUtils.isBlank(tagValue)) {
			    continue;
			}
			tagValueList.add(tagValue);
		    } else if (name.startsWith(Constants.TRUST_TAG)) {
                        trustTag = name;
			if (StringUtils.isBlank(trustTag)) {
			    continue;
			}
			List<XMLObject> attributeValues = attribute.getAttributeValues();
			for (XMLObject xmlObject : attributeValues) {
			    Element dom = xmlObject.getDOM();
			    trustTagValue = dom.getTextContent();
			}
			if (StringUtils.isBlank(trustTagValue)) {
			    continue;
			}
			if (trustTagValueList.containsKey(trustTag)) {
			    trustTagValue = trustTagValueList.get(trustTag);
			} else {
			    trustTagValueList.put(trustTag, trustTagValue);
			}
		    }
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
		    String writeValueAsString = mapper.writeValueAsString(assetTagToValueMap);
		    ahHost.setAssetTags(writeValueAsString);
		} catch (JsonProcessingException e) {
		    log.error("Error converting map of asset tags to JSON");
		}
	    }

	}
	if (citHostReport != null) {
	    ahHost.setSamlReport(citHostReport.getSaml());
	    FlavorStatusResponse trustResponse = convertToHostTrustResponse(citHost, trustTagValueList);
	    ObjectMapper objectMapper = new ObjectMapper();
	    try {
		ahHost.setTrustTagsJson(objectMapper.writeValueAsString(trustResponse));
	    } catch (JsonProcessingException e) {
		log.error(
			"Unable to parse the 'host_trust_response' from the host attestation response for host: {} and name: {}",
			citHost.getId(), citHost.getHostName());
	    }
	}

	return ahHost;
    }

    public static AhHost mapHostToAhHost(MWHost host, AhHost ahHost) {
	log.info("Before getting logged in user");
	String currentUser = ShiroUtil.subjectUsername();
	log.info("After getting logged in user");
	return mapHostToAhHost(host, ahHost, currentUser);

    }

    public static Host mapAhHostToCitHost(AhHost ahHost) throws CertificateException {
	Host host = new Host();
	host.setId(new UUID(ahHost.getId().getBytes()));
//	host.setBiosMleUuid(ahHost.getBiosMleUuid());
	host.setConnectionString(ahHost.getConnectionUrl());
	host.setHardwareUuid(UUID.valueOf(ahHost.getHardwareUuid()));
	host.setHostName(ahHost.getHostName());
	return host;
    }
    
    public static FlavorStatusResponse convertToHostTrustResponse(Host citHost, Map<String, String> trustTagValueList) {
        FlavorStatusResponse trustResponse = new FlavorStatusResponse();
        trustResponse.hostname = new Hostname(citHost.getHostName());
        trustResponse.trust = trustTagValueList;
        return trustResponse;
    }
}
