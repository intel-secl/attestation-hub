/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.controller.AhTenantPluginCredentialJpaController;
import com.intel.mtwilson.attestationhub.data.AhTenantPluginCredential;
import org.apache.commons.lang.StringUtils;

import com.intel.attestationhub.api.HostFilterCriteria;
import com.intel.attestationhub.api.MWHost;
import com.intel.attestationhub.api.MappingResultResponse;
import com.intel.attestationhub.api.SearchCriteriaForMapping;
import com.intel.attestationhub.api.Tenant;
import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.attestationhub.api.Tenant.PluginProperty;
import com.intel.attestationhub.api.TenantFilterCriteria;
import com.intel.attestationhub.mapper.HostMapper;
import com.intel.attestationhub.mapper.TenantMapper;
import com.intel.attestationhub.service.AttestationHubService;
import com.intel.mtwilson.flavor.rest.v2.model.Host;
import com.intel.mtwilson.attestationhub.controller.AhHostJpaController;
import com.intel.mtwilson.attestationhub.controller.AhMappingJpaController;
import com.intel.mtwilson.attestationhub.controller.AhTenantJpaController;
import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.controller.exceptions.PreexistingEntityException;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.attestationhub.data.AhMapping;
import com.intel.mtwilson.attestationhub.data.AhTenant;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.attestationhub.service.PersistenceServiceFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class AttestationHubServiceImpl implements AttestationHubService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttestationHubServiceImpl.class);

    public static AttestationHubService getInstance() {
	return new AttestationHubServiceImpl();
    }

	public static boolean dataEncryptionKeyConfigured() {
		log.debug("Checking whether encryption key is configured");
		String dekBase64 = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_DATA_ENCRYPTION_KEY);
		if (dekBase64 == null || dekBase64.isEmpty()) {
			log.debug("encryption key is not configured");
			return false;
		}
		log.debug("encryption key is configured");
		return true;
	}

	// Extract plugin credentials from tenant configuration
	private  Map<String,List<Tenant.PluginProperty>> extractCredentialFromTenant(Tenant tenant) {
		log.debug("Extracting credentials from tenant configuration");
		List<Tenant.PluginProperty> credentials = new ArrayList<>();
		Map<String,List<Tenant.PluginProperty>> pluginCredentialsMap = new HashMap<>();

		List<Plugin> plugins = tenant.getPlugins();

		for (Plugin plugin : plugins) {
			if(plugin.getName().equalsIgnoreCase("nova") && plugin.getProperty("user.name") != null && plugin.getProperty("user.password") !=null) {
				credentials.add(plugin.getProperty("user.name"));
				credentials.add(plugin.getProperty("user.password"));
				pluginCredentialsMap.put(plugin.getName(), credentials);
			}
			if (plugin.getName().equalsIgnoreCase("kubernetes")
		                && plugin.getProperty("kubernetes.client.keystore.password") != null
                		&& plugin.getProperty("kubernetes.server.keystore.password") !=null) {
		      	        	credentials.add(plugin.getProperty("kubernetes.client.keystore.password"));
                			credentials.add(plugin.getProperty("kubernetes.server.keystore.password"));
			                pluginCredentialsMap.put(plugin.getName(), credentials);
		        }
		}
		return pluginCredentialsMap;
	}

	// Removing plugin credentials before storing tenant to database
	private  void removeCredentialFromTenant(Tenant tenant) {
		log.debug("Removing credentials from tenant");

		List<Plugin> plugins = tenant.getPlugins();

		for (Plugin plugin : plugins) {
			if(plugin.getName().equalsIgnoreCase("nova") && plugin.getProperty("user.name") != null &&
					plugin.getProperty("user.password") !=null) {
				plugin.removeProperty("user.name");
				plugin.removeProperty("user.password");
			}
			if (plugin.getName().equalsIgnoreCase("kubernetes")
					&& plugin.getProperty("kubernetes.client.keystore.password") != null
					&& plugin.getProperty("kubernetes.server.keystore.password") !=null) {
				plugin.removeProperty("kubernetes.client.keystore.password");
				plugin.removeProperty("kubernetes.server.keystore.password");
			}
		}
	}

	private void createTenantPluginCredential(AhTenant ahTenant, Map<String,List<PluginProperty>> pluginCredentialsMap) throws AttestationHubException {
		PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
		AhTenantPluginCredentialJpaController tenantPluginCredentialController = persistenceServiceFactory.getTenantPluginCredentialController();

		Set<String> plugins = pluginCredentialsMap.keySet();
		for (String plugin : plugins) {
			try {
				AhTenantPluginCredential ahTenantPluginCredential = new AhTenantPluginCredential();
				ahTenantPluginCredential.setId(new UUID().toString());
				ahTenantPluginCredential.setTenantId(ahTenant.getId());
				ahTenantPluginCredential.setPluginName(plugin);
				ahTenantPluginCredential.setTenantName(ahTenant.getTenantName());
				ahTenantPluginCredential.setCredential(pluginCredentialsMap.get(plugin));
				ahTenantPluginCredential.setCreatedTs(Calendar.getInstance().getTime());
				tenantPluginCredentialController.create(ahTenantPluginCredential);
			} catch (Exception e) {
				String msg = "Error saving tenant plugin credential : " + plugin;
				log.error(msg, e);
				throw new AttestationHubException(msg, e);
			}
		}
	}

	private void deleteTenantPluginCredential(AhTenant ahTenant) throws AttestationHubException {
		PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
		AhTenantPluginCredentialJpaController tenantPluginCredentialController = persistenceServiceFactory.getTenantPluginCredentialController();

		List<AhTenantPluginCredential> ahTenantPluginCredentials = tenantPluginCredentialController.findByTenantId(ahTenant.getId());
		for (AhTenantPluginCredential ahTenantPluginCredential : ahTenantPluginCredentials) {
			try {
				tenantPluginCredentialController.destroy(ahTenantPluginCredential.getId());
			} catch (NonexistentEntityException e) {
				String msg = "Invalid id: " + ahTenantPluginCredential.getId();
				log.error(msg, e);
				throw new AttestationHubException(msg, e);
			} catch (Exception e) {
				String msg = "Error deleting tenant plugin credential : " + ahTenantPluginCredential.getId();
				log.error(msg, e);
				throw new AttestationHubException(msg, e);
			}
		}
	}

    @Override
    public String createTenant(Tenant tenant) throws AttestationHubException {
	String newTenantId = null;
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = persistenceServiceFactory.getTenantController();
	Map<String,List<Tenant.PluginProperty>> pluginCredentialsMap = extractCredentialFromTenant(tenant);
	removeCredentialFromTenant(tenant);
	AhTenant ahTenant = TenantMapper.mapApiToJpa(tenant);
	try {
		if (dataEncryptionKeyConfigured()) {
			tenantController.create(ahTenant);
			createTenantPluginCredential(ahTenant, pluginCredentialsMap);
			newTenantId = ahTenant.getId();
			tenant.setId(newTenantId);
		} else {
			log.error("Cannot create tenant, data encryption key is not configured");
			throw new AttestationHubException("Cannot create tenant, data encryption key is not configured");
		}
	} catch (Exception e) {
	    log.error("Error saving the tenant");
	    throw new AttestationHubException("Error saving the tenant", e);
	}
	return newTenantId;
    }

    @Override
    public Tenant retrieveTenant(String tenantId) throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = persistenceServiceFactory.getTenantController();
	AhTenant ahTenant = tenantController.findAhTenant(tenantId);
	if (ahTenant == null) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Tenant with id: " + tenantId + " does not exist");
	    throw new AttestationHubException(nonexistentEntityException);
	}
	if (ahTenant.getDeleted() != null && ahTenant.getDeleted()) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Tenant with id: " + tenantId + " was deleted.");
	    throw new AttestationHubException(nonexistentEntityException);
	}
	Tenant newTenant = TenantMapper.mapJpatoApi(ahTenant);
	return newTenant;
    }

    @Override
    public List<Tenant> retrieveAllTenants() throws AttestationHubException {
	List<Tenant> tenants = new ArrayList<Tenant>();
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = persistenceServiceFactory.getTenantController();
	List<AhTenant> ahTenants = tenantController.findAhTenantEntities();

	if (ahTenants == null) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Tenants do not exist");
	    throw new AttestationHubException(nonexistentEntityException);
	}

	for (AhTenant ahTenant : ahTenants) {
	    if (ahTenant.getDeleted() != null && ahTenant.getDeleted()) {
		log.debug("Tenant {} is deleted. Hence not returning in the results", ahTenant.getId());
		continue;
	    }
	    tenants.add(TenantMapper.mapJpatoApi(ahTenant));
	}

	return tenants;
    }

    @Override
    public Tenant updateTenant(Tenant tenant) throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = persistenceServiceFactory.getTenantController();
	Map<String,List<Tenant.PluginProperty>> pluginCredentialsMap = extractCredentialFromTenant(tenant);
	removeCredentialFromTenant(tenant);
	AhTenant ahTenant = TenantMapper.mapApiToJpa(tenant);
	try {
	    AhTenant existingTenant = tenantController.findAhTenant(tenant.getId());
	    if (existingTenant.getDeleted() != null && existingTenant.getDeleted()) {
		throw new AttestationHubException("Unable to edit a tenant that has been marked as deleted");
	    }
	    if (dataEncryptionKeyConfigured()) {
			tenantController.edit(ahTenant);
			deleteTenantPluginCredential(ahTenant);
			createTenantPluginCredential(ahTenant, pluginCredentialsMap);
		} else {
			log.error("Cannot update tenant, data encryption key is not configured");
			throw new AttestationHubException("Cannot update tenant, data encryption key is not configured");
		}
	} catch (NonexistentEntityException e) {
	    String msg = "Invalid id: " + tenant.getId();
	    log.error(msg, e);
	    throw new AttestationHubException(msg, e);
	} catch (AttestationHubException e) {
	    log.error("Error", e);
	    throw e;
	} catch (Exception e) {
	    String msg = "Error updating tenant : " + tenant.getId();
	    log.error(msg, e);
	    throw new AttestationHubException(msg, e);
	}
	return tenant;
    }

    @Override
    public void deleteTenant(String tenantId) throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = persistenceServiceFactory.getTenantController();
	AhTenant ahTenant = tenantController.findAhTenant(tenantId);
	ahTenant.setDeleted(true);
	try {
	    tenantController.edit(ahTenant);
	    Collection<AhMapping> ahMappingCollection = ahTenant.getAhMappingCollection();
	    // delete corresponding mappings
	    for (AhMapping ahMapping : ahMappingCollection) {
		deleteMapping(ahMapping.getId());
	    }
	} catch (NonexistentEntityException e) {
	    String msg = "Invalid id: " + tenantId;
	    log.error(msg, e);
	    throw new AttestationHubException(msg, e);
	} catch (Exception e) {
	    String msg = "Error deleting tenant : " + tenantId;
	    log.error(msg, e);
	    throw new AttestationHubException(msg, e);
	}
    }

    @Override
    public AhMapping retrieveMapping(String mappingId) throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhMappingJpaController mappingController = persistenceServiceFactory.getTenantToHostMappingController();
	AhMapping ahMapping = mappingController.findAhMapping(mappingId);
	if (ahMapping == null) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Tenant-Host mapping with id: " + mappingId + " does not exist");
	    throw new AttestationHubException(nonexistentEntityException);
	}
	if (ahMapping.getDeleted() != null && ahMapping.getDeleted()) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Tenant-Host mapping with id: " + mappingId + " has been deleted");
	    throw new AttestationHubException(nonexistentEntityException);
	}
	return ahMapping;
    }

    @Override
    public List<AhMapping> retrieveAllMappings() throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhMappingJpaController mappingController = persistenceServiceFactory.getTenantToHostMappingController();
	List<AhMapping> ahMappings = mappingController.findAhMappingEntities();
	if (ahMappings == null) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Tenant-Host mappings do not exist");
	    throw new AttestationHubException(nonexistentEntityException);
	}
	List<AhMapping> activeMappings = new ArrayList<>();
	for (AhMapping ahMapping : ahMappings) {
	    if (ahMapping.getDeleted() != null && ahMapping.getDeleted()) {
		log.debug(
			"Mapping {} between tenant: {} and host hardware uuid: {} has been deleted. Skipping from all mapping result",
			ahMapping.getId(), ahMapping.getTenant().getId(), ahMapping.getHostHardwareUuid());
		continue;
	    }
	    activeMappings.add(ahMapping);
	}
	return activeMappings;
    }

    @Override
    public MappingResultResponse createOrUpdateMapping(String tenantId, List<String> hostHardwareUuids)
	    throws AttestationHubException {
	MappingResultResponse mappingResultResponse = new MappingResultResponse();

	PersistenceServiceFactory tenantPersistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = tenantPersistenceServiceFactory.getTenantController();

	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhMappingJpaController mappingController = persistenceServiceFactory.getTenantToHostMappingController();

	AhTenant ahTenant = tenantController.findAhTenant(tenantId);
	// Throw an exception if the tennat id is non existent in hub db
	if (ahTenant == null) {
	    throw new AttestationHubException("Tenant does not exist with id : " + tenantId);
	}
	Collection<AhMapping> ahMappingCollection = ahTenant.getAhMappingCollection();
	Set<String> uniqueHostHardwareIds = new HashSet<String>(hostHardwareUuids);

	// Update existing mappings if any
	log.debug("Number of unique hardware uuids: {}", uniqueHostHardwareIds.size());
	for (AhMapping ahMapping : ahMappingCollection) {
	    if (StringUtils.isNotBlank(ahMapping.getHostHardwareUuid())
		    && uniqueHostHardwareIds.contains(ahMapping.getHostHardwareUuid())) {
		log.debug("Mapping between tenant: {} and host hardware uuid: {} already exists",
			ahMapping.getTenant().getId(), ahMapping.getHostHardwareUuid());
		if (ahMapping.getDeleted() != null && ahMapping.getDeleted()) {
		    log.debug("The mapping: {} was deleted earlier. Reactivating it instead of creating a new one",
			    ahMapping.getId());
		    ahMapping.setDeleted(false);
		    try {
			mappingController.edit(ahMapping);
		    } catch (NonexistentEntityException e) {
			String msg = "Invalid mapping id : " + ahMapping.getId();
			log.error(msg, e);
			throw new AttestationHubException(msg, e);
		    } catch (Exception e) {
			String msg = "Error updating tenant-host mapping";
			log.error(msg, e);
			throw new AttestationHubException(msg, e);
		    }
		} else {
		    log.debug("Mapping: {} is still active. Not creating a new entry", ahMapping.getId());
		}
		// remove the hardware uuid so that duplicate entry is not
		// created
		uniqueHostHardwareIds.remove(ahMapping.getHostHardwareUuid());
		MappingResultResponse.TenantToHostMapping mapping = new MappingResultResponse.TenantToHostMapping();
		mapping.hostHardwareUuid = ahMapping.getHostHardwareUuid();
		mapping.mappingId = ahMapping.getId();
		mapping.tenantId = ahTenant.getId();
		mappingResultResponse.mappings.add(mapping);
	    }
	}
	log.debug("Creating mappings for remaning hardware uuids: {}", uniqueHostHardwareIds.size());
	// Create hosts mappings which do not exist for the tenant
	for (String hostHardwareUuid : uniqueHostHardwareIds) {
	    AhMapping ahMapping = new AhMapping();
	    ahMapping.setHostHardwareUuid(hostHardwareUuid);
	    ahMapping.setTenant(ahTenant);
	    ahMapping.setDeleted(false);
	    try {
		mappingController.create(ahMapping);
		MappingResultResponse.TenantToHostMapping mapping = new MappingResultResponse.TenantToHostMapping();
		mapping.hostHardwareUuid = hostHardwareUuid;
		mapping.mappingId = ahMapping.getId();
		mapping.tenantId = ahTenant.getId();
		mappingResultResponse.mappings.add(mapping);
	    } catch (PreexistingEntityException e) {
		String msg = "Mapping id " + ahMapping.getId()
			+ " already exists. Cannot create a new one with the same id";
		log.error(msg, e);
		throw new AttestationHubException(msg, e);
	    } catch (Exception e) {
		String msg = "Error creating tenant-host mapping for tenant: " + tenantId + " and host: "
			+ hostHardwareUuid;
		log.error(msg, e);
		throw new AttestationHubException(msg, e);
	    }
	}
	return mappingResultResponse;
    }

    @Override
    public void deleteMapping(String mappingId) throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhMappingJpaController mappingController = persistenceServiceFactory.getTenantToHostMappingController();
	AhMapping ahMapping = mappingController.findAhMapping(mappingId);
	if (ahMapping == null) {
	    String msg = "Tenant-Host mapping with id: " + mappingId + " does not exist";
	    log.error(msg);
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(msg);
	    throw new AttestationHubException(nonexistentEntityException);
	}
	ahMapping.setDeleted(true);
	try {
	    mappingController.edit(ahMapping);
	} catch (NonexistentEntityException e) {
	    String msg = "Tenant-Host mapping with id: " + mappingId + " does not exist";
	    throw new AttestationHubException(msg, e);
	} catch (Exception e) {
	    log.error("Unable to update the delete flag for mapping : {}", mappingId);
	    throw new AttestationHubException(e);
	}
    }

    /**
     * this method is called from the poller. This will create / edit hosts in
     * the attestation hub db based on the response from the attestation
     * service.
     * 
     * @param hostAttestationsMap
     * @throws AttestationHubException
     */
    @Override
    public void saveHosts(Map<String, MWHost> hostAttestationsMap) throws AttestationHubException {
	log.info("Saving hosts returned by Attestation Service");
	if (hostAttestationsMap == null || hostAttestationsMap.size() == 0) {
	    log.info("No hosts retrieved from attestation service to be inserted in hub db");
	    return;
	}

	AhHostJpaController hostController = PersistenceServiceFactory.getInstance().getHostController();
	List<String> savedAhHostIdList = new ArrayList<String>(hostAttestationsMap.size());
	log.debug("Fetch all hosts from the attestation hub DB");

	// the map passed to method has the key which is the ID of the
	// host in the attestation db. the value of the map is a custom hub
	// wrapper object
	// which contains the Host and Host Attestation object for the host from
	// Attestation service
	for (String id : hostAttestationsMap.keySet()) {
	    boolean ahHostExists = false;
	    MWHost mwHost = hostAttestationsMap.get(id);
	    Host host = mwHost.getHost();
	    log.debug("Processing save for host ID : {} and name: {}", host.getId(), host.getHostName());
	    AhHost ahHost = hostController.findAhHost(host.getId().toString());
	    log.debug("Does the host already exist in Attestation Hub DB ? {}", ahHost != null);

	    // In a case where a host was added previously, but later was
	    // deleted from MTW and re added,
	    // the UUID would be different, but the hardware uuid would be the
	    // same. In this case
	    // we would want to disable the earlier record
	    if (ahHost != null) {
		ahHostExists = true;
	    } else {
		List<AhHost> findHostsByHardwareUuid = hostController.findHostsByHardwareUuid(host.getHardwareUuid().toString().toUpperCase());
		if (findHostsByHardwareUuid != null) {
		    // In this case we want to disable all these records
		    for (AhHost ahHost2 : findHostsByHardwareUuid) {
		    	if(ahHost2.getDeleted() != null && ahHost2.getDeleted()){
		    		continue;
				}
			ahHost2.setDeleted(true);
			ahHost2.setModifiedDate(new Date());
			try {
			    hostController.edit(ahHost2);
			} catch (PreexistingEntityException e) {
			    log.error("Error updating host {} since host already exists", ahHost2.getId(), e);
			    throw new AttestationHubException(e);
			} catch (Exception e) {
			    log.error("Error updating host", e);
			    throw new AttestationHubException(e);
			}
		    }
		}
	    }
	    ahHost = HostMapper.mapHostToAhHost(mwHost, ahHost, "admin");
	    ahHost.setDeleted(false);

	    try {
		if (ahHostExists) {
		    hostController.edit(ahHost);
		    log.debug("Edited the host: {} in attestation DB ", ahHost.getId());
		    savedAhHostIdList.add(ahHost.getId());
		} else {
		    hostController.create(ahHost);
		    log.debug("Added host to attestation DB with ID: {}", ahHost.getId());
		}
	    } catch (PreexistingEntityException e) {
		log.error("Error creating host {} since host already exists", ahHost.getId(), e);
		throw new AttestationHubException(e);
	    } catch (Exception e) {
		log.error("Error creating host", e);
		throw new AttestationHubException(e);
	    }
	}
    }

    @Override
    public List<Tenant> searchTenantsBySearchCriteria(TenantFilterCriteria tenantFilterCriteria)
	    throws AttestationHubException {
	AhTenantJpaController tenantController = PersistenceServiceFactory.getInstance().getTenantController();
	List<Tenant> tenantsList = new ArrayList<Tenant>();
	List<AhTenant> ahTenantsList = tenantController
		.findAhTenantsByNameSearchCriteria(tenantFilterCriteria.nameEqualTo.toUpperCase());
	if (ahTenantsList != null) {
	    log.debug("Found {} tenants with name : {}", ahTenantsList.size(), tenantFilterCriteria.nameEqualTo);
	    for (AhTenant ahTenant : ahTenantsList) {
		if (ahTenant.getDeleted() != null && ahTenant.getDeleted()) {
		    log.debug("Tenant {} has been deleted. Skipping from search results", ahTenant.getId());
		    continue;
		}
		tenantsList.add(TenantMapper.mapJpatoApi(ahTenant));
	    }
	}
	return tenantsList;
    }

    @Override
    public List<AhMapping> searchMappingsBySearchCriteria(SearchCriteriaForMapping criteriaForMapping)
	    throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhMappingJpaController mappingController = persistenceServiceFactory.getTenantToHostMappingController();
	List<AhMapping> ahMappings = new ArrayList<>();
	if (StringUtils.isNotBlank(criteriaForMapping.tenantId)) {
	    ahMappings = mappingController.findAhMappingsByTenantId(criteriaForMapping.tenantId);
	} else if (StringUtils.isNotBlank(criteriaForMapping.hostHardwareUuid)) {
	    ahMappings = mappingController.findAhMappingsByHostHardwareUuid(criteriaForMapping.hostHardwareUuid);
	} else {
	    ahMappings = null;
	}
	if (ahMappings == null) {
	    return ahMappings;
	}
	List<AhMapping> activeMappings = new ArrayList<>();
	for (AhMapping ahMapping : ahMappings) {
	    if (ahMapping.getDeleted() != null && ahMapping.getDeleted()) {
		log.debug(
			"Mapping {} between tenant: {} and host hardware uuid: {} has been deleted. Skipping from all mapping result",
			ahMapping.getId(), ahMapping.getTenant().getId(), ahMapping.getHostHardwareUuid());
		continue;
	    }
	    activeMappings.add(ahMapping);
	}
	return activeMappings;
    }

    @Override
    public AhHost getHostById(String id) throws AttestationHubException {
	log.info("Finding host with given ID {}", id);
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhHostJpaController ahHostJpaController = persistenceServiceFactory.getHostController();
	AhHost ahHost = ahHostJpaController.findAhHost(id);
	if (ahHost == null) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Host with id: " + id + " does not exist");
	    throw new AttestationHubException(nonexistentEntityException);
	}
	if (ahHost.getDeleted() != null && ahHost.getDeleted()) {
	    log.debug("The host was marked as deleted. Hence returning 404");
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Host with id: " + id + " was deleted");
	    throw new AttestationHubException(nonexistentEntityException);
	}

	return ahHost;
    }

    @Override
    public List<AhHost> getHosts() throws AttestationHubException {
	log.info("Getting all available hosts on attestation hub");
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhHostJpaController ahHostJpaController = persistenceServiceFactory.getHostController();
	List<AhHost> ahHosts = ahHostJpaController.findAhHostEntities();
	List<AhHost> activeHosts = null;
	if (ahHosts != null) {
	    activeHosts = new ArrayList<>();
	    for (AhHost ahHost : ahHosts) {
		if (ahHost.getDeleted() != null && ahHost.getDeleted()) {
		    log.debug("The host was marked as deleted. Hence skipping.");
		    continue;
		}
		activeHosts.add(ahHost);
	    }
	}
	return activeHosts;
    }

    @Override
    public List<AhHost> searchHostsWithSearchCriteria(HostFilterCriteria hostFilterCriteria)
	    throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhHostJpaController ahHostJpaController = persistenceServiceFactory.getHostController();
	List<AhHost> ahHosts = ahHostJpaController.findHostsWithFilterCriteria(hostFilterCriteria.nameEqualTo);
	List<AhHost> activeHosts = null;
	if (ahHosts != null) {
	    log.debug("Found {} host with given filter criteria {}", ahHosts.size(), hostFilterCriteria.nameEqualTo);
	    activeHosts = new ArrayList<>();

	    for (AhHost ahHost : ahHosts) {
		if (ahHost.getDeleted() != null && ahHost.getDeleted()) {
		    log.debug("The host was marked as deleted. Hence skipping.");
		    continue;
		}
		activeHosts.add(ahHost);
	    }
	} else {
	    log.debug("No hosts found with given filter criteria {}", hostFilterCriteria.nameEqualTo);
	}
	return activeHosts;
    }

    @Override
    public List<AhHost> findHostsByHardwareUuid(String hardwareUuid) throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhHostJpaController ahHostJpaController = persistenceServiceFactory.getHostController();
	hardwareUuid = hardwareUuid.toUpperCase();
	List<AhHost> ahHosts = ahHostJpaController.findHostsByHardwareUuid(hardwareUuid);
	List<AhHost> activeHosts = null;
	if (ahHosts != null) {
	    log.debug("Found {} host with given hardware id {}", ahHosts.size(), hardwareUuid);
	    for (AhHost ahHost : ahHosts) {
		if (ahHost.getDeleted() != null && ahHost.getDeleted()) {
		    log.debug("Host: {} has been deleted", ahHost.getId());
		    continue;
		} else {
		    if (activeHosts == null) {
			activeHosts = new ArrayList<>();
		    }
		}
		activeHosts.add(ahHost);
	    }

	} else {
	    log.debug("No hosts found with given host hardware uuid {}", hardwareUuid);
	}

	return activeHosts;

    }

    @Override
    public AhHost findActiveHostByHardwareUuid(String hardwareUuid) throws AttestationHubException {
	List<AhHost> findHostsByHardwareUuid = findHostsByHardwareUuid(hardwareUuid);
	log.info("Finding active host");

	if (findHostsByHardwareUuid == null) {
	    throw new AttestationHubException("Unable to find an active host with hardware id: " + hardwareUuid);
	}
	AhHost ahHost = findHostsByHardwareUuid.get(0);
	return ahHost;
    }

	public void markExpiredHostsAsDeleted() {
		PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
		AhHostJpaController ahHostJpaController = persistenceServiceFactory.getHostController();
		List<AhHost> ahHosts = ahHostJpaController.findAhHostEntities();
		DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
		for (AhHost ahHost : ahHosts) {
			if (DateTime.now().isAfter(formatter.parseDateTime(ahHost.getValidTo()))) {
				ahHost.setDeleted(true);
				try {
					ahHostJpaController.edit(ahHost);
				} catch (NonexistentEntityException e) {
					String msg = "Invalid host id: " + ahHost.getId();
					log.error(msg, e);
				} catch (Exception e) {
					String msg = "Error updating host as deleted: " + ahHost.getId();
					log.error(msg, e);
				}
			}
		}
	}


	@Override
	public void markExpiredHostsAsUntrusted() {
		PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
		AhHostJpaController ahHostJpaController = persistenceServiceFactory.getHostController();
		List<AhHost> ahHosts = ahHostJpaController.findAhHostEntities();
		DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
		for (AhHost ahHost : ahHosts) {
			if (DateTime.now().isAfter(formatter.parseDateTime(ahHost.getValidTo()))) {
				ahHost.setTrusted(false);
				try {
					ahHostJpaController.edit(ahHost);
				} catch (NonexistentEntityException e) {
					String msg = "Invalid host id: " + ahHost.getId();
					log.error(msg, e);
				} catch (Exception e) {
					String msg = "Error updating host as untrusted: " + ahHost.getId();
					log.error(msg, e);
				}
			}
		}
	}
}
