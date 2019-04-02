/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.attestationhub.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.attestationhub.api.Tenant.PluginProperty;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.util.ASDataCipher;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rawatar
 */
@Entity
@Table(name = "ah_tenant_plugin_credential")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "AhTenantPluginCredential.findAll", query = "SELECT a FROM AhTenantPluginCredential a"),
        @NamedQuery(name = "AhTenantPluginCredential.findById", query = "SELECT a FROM AhTenantPluginCredential a WHERE a.id = :id ORDER BY a.createdTs DESC"),
        @NamedQuery(name = "AhTenantPluginCredential.findByTenantId", query = "SELECT a FROM AhTenantPluginCredential a WHERE a.tenantId = :tenantId ORDER BY a.createdTs DESC"),
        @NamedQuery(name = "AhTenantPluginCredential.findByTenantName", query = "SELECT a FROM AhTenantPluginCredential a WHERE a.tenantName = :tenantName ORDER BY a.createdTs DESC"),
        @NamedQuery(name = "AhTenantPluginCredential.findByTenantIdAndPluginName", query = "SELECT a FROM AhTenantPluginCredential a WHERE a.tenantId = :tenantId and a.pluginName = :pluginName ORDER BY a.createdTs DESC")})
public class AhTenantPluginCredential implements Serializable {
    @Transient
    private transient Logger log = LoggerFactory.getLogger(getClass());

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "id")
    private String id;
    @Column(name = "tenant_id")
    private String tenantId;
    @Column(name = "plugin_name")
    private String pluginName;
    @Column(name = "tenant_name")
    private String tenantName;
    @Basic(optional = false)
    @Column(name = "credential")
    private String credential;
    @Column(name = "created_ts")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdTs;

    @Transient
    private transient List<PluginProperty> credentialInPlain; // the decrypted version

    public AhTenantPluginCredential() {
    }

    public AhTenantPluginCredential(String id) {
        this.id = id;
    }

    public AhTenantPluginCredential(String id, String name) {
        this.id = id;
        this.tenantName = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public List<PluginProperty> getCredential() throws AttestationHubException {
        if (credentialInPlain == null && credential != null) {
            try {
                String decryptedCredential = ASDataCipher.cipher.decryptString(credential);
                ObjectMapper mapper = new ObjectMapper();
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                //credentialInPlain = mapper.readValue(decryptedCredential, List.class);
                List<Object> objects = mapper.readValue(decryptedCredential, List.class);
                List<PluginProperty> properties = new ArrayList<>();
                for(Object object : objects) {
                    LinkedHashMap list = (LinkedHashMap) object;
                    List<String> keyValues = new ArrayList<>();
                    for(Object entry : list.values()) {
                        keyValues.add((String)entry);
                    }
                    PluginProperty property = getProperty(keyValues);
                    properties.add(property);
                }
                credentialInPlain = properties;
                //log.debug("AhTenantPluginCredential ASDataCipher plainText = {}", passwordInPlain);
                log.debug("AhTenantPluginCredential ASDataCipher cipherText = {}", credential);
            } catch (JsonGenerationException e) {
                log.error("Error generating credential json", e);
                throw new AttestationHubException(e);
            } catch (JsonMappingException e) {
                log.error("Error mapping credential json", e);
                throw new AttestationHubException(e);
            } catch (IOException e) {
                log.error("Error creating credential json", e);
                throw new AttestationHubException(e);
            } catch (Exception e) {
                log.error("Cannot decrypt tenant plugin credentials", e);
                throw new IllegalArgumentException("Cannot decrypt tenant plugin credentials.");
            }
        }
        return credentialInPlain;
    }

    public void setCredential(List<PluginProperty> credential) throws AttestationHubException {
        this.credentialInPlain = credential;
        if (credentialInPlain == null) {
            this.credential = null;
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                String plainCredential = mapper.writeValueAsString(credentialInPlain);
                this.credential = ASDataCipher.cipher.encryptString(plainCredential);
            } catch (JsonGenerationException e) {
                log.error("Error generating credential json", e);
                throw new AttestationHubException(e);
            } catch (JsonMappingException e) {
                log.error("Error mapping credential json", e);
                throw new AttestationHubException(e);
            } catch (IOException e) {
                log.error("Error creating credential json", e);
                throw new AttestationHubException(e);
            } catch (Exception e) {
                log.error("Error saving credential json", e);
                throw new AttestationHubException(e);
            }
        }
    }

    private PluginProperty getProperty(List<String> values) {
        return new PluginProperty(values.get(0), values.get(1));
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public Date getCreatedTs() {
        return createdTs;
    }

    public void setCreatedTs(Date createdTs) {
        this.createdTs = createdTs;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AhTenantPluginCredential)) {
            return false;
        }
        AhTenantPluginCredential other = (AhTenantPluginCredential) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.intel.mtwilson.attestationhub.data.AhTenantPluginCredential[ id=" + id + " Tenant=" + tenantName + " Plugin=" + pluginName + " ]";
    }

}
