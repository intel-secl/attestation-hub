/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.attestationhub.controller;

import com.intel.dcsg.cpg.crypto.CryptographyException;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.controller.exceptions.PreexistingEntityException;
import com.intel.mtwilson.attestationhub.data.AhTenantPluginCredential;
import com.intel.dcsg.cpg.crypto.Aes128;
import com.intel.mtwilson.util.ASDataCipher;
import com.intel.mtwilson.util.Aes128DataCipher;
import org.apache.commons.codec.binary.Base64;

import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 *
 * @author rawatar
 */
public class AhTenantPluginCredentialJpaController implements Serializable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AhTenantPluginCredentialJpaController.class);

    public AhTenantPluginCredentialJpaController(EntityManagerFactory emf) {
        this.emf = emf;
        initDataEncryptionKey();
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void initDataEncryptionKey() {
        log.debug("Initializing encryption key");
        try {
            String dekBase64 = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_DATA_ENCRYPTION_KEY);
            if (dekBase64 == null || dekBase64.isEmpty()) {
                log.error("Cannot start server, data encryption key is not defined");
            }
            ASDataCipher.cipher = new Aes128DataCipher(new Aes128(Base64.decodeBase64(dekBase64)));
        }
        catch(CryptographyException e) {
            throw new IllegalArgumentException("Cannot initialize data encryption cipher", e);
        }

        log.debug("Initialized encryption key: {}", ASDataCipher.cipher.getClass().getName());
    }

    public void create(AhTenantPluginCredential ahTenantPluginCredential) throws PreexistingEntityException, Exception {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(ahTenantPluginCredential);
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (findAhTenantCredential(ahTenantPluginCredential.getId()) != null) {
                throw new PreexistingEntityException("AhTenantPluginCredential " + ahTenantPluginCredential + " already exists.", ex);
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public void edit(AhTenantPluginCredential ahTenantPluginCredential) throws NonexistentEntityException, Exception {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(ahTenantPluginCredential);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                String id = ahTenantPluginCredential.getId();
                if (findAhTenantCredential(id) == null) {
                    throw new NonexistentEntityException("The AhTenantPluginCredential with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public void destroy(String id) throws NonexistentEntityException {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            AhTenantPluginCredential ahTenantPluginCredential;
            try {
                ahTenantPluginCredential = em.getReference(AhTenantPluginCredential.class, id);
                ahTenantPluginCredential.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The AhTenantPluginCredential with id " + id + " no longer exists.", enfe);
            }
            em.remove(ahTenantPluginCredential);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public List<AhTenantPluginCredential> findAhTenantCredentialEntities() {
        return findAhTenantCredentialEntities(true, -1, -1);
    }

    public List<AhTenantPluginCredential> findAhTenantCredentialEntities(int maxResults, int firstResult) {
        return findAhTenantCredentialEntities(false, maxResults, firstResult);
    }

    private List<AhTenantPluginCredential> findAhTenantCredentialEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(AhTenantPluginCredential.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public AhTenantPluginCredential findAhTenantCredential(String id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(AhTenantPluginCredential.class, id);
        } finally {
            em.close();
        }
    }

    public List<AhTenantPluginCredential> findByTenantId(String id) {

        List<AhTenantPluginCredential> tenantPluginCredentials = null;
        EntityManager em = getEntityManager();
        try {
            Query query = em.createNamedQuery("AhTenantPluginCredential.findByTenantId");
            query.setParameter("tenantId", id);
            if (query.getResultList() != null && !query.getResultList().isEmpty()) {
                tenantPluginCredentials = query.getResultList();
            }
            return tenantPluginCredentials;
        } finally {
            em.close();
        }
    }

    public List<AhTenantPluginCredential> findByTenantName(String tenantName) {

        List<AhTenantPluginCredential> tenantPluginCredentials = null;
        EntityManager em = getEntityManager();
        try {
            Query query = em.createNamedQuery("AhTenantPluginCredential.findByTenantName");
            query.setParameter("tenantName", tenantName);
            if (query.getResultList() != null && !query.getResultList().isEmpty()) {
                tenantPluginCredentials = query.getResultList();
            }
            return tenantPluginCredentials;
        } finally {
            em.close();
        }
    }

    public AhTenantPluginCredential findByTenantIdAndPluginName(String tenantId, String pluginName) {

        AhTenantPluginCredential tenant = null;
        EntityManager em = getEntityManager();
        try {

            Query query = em.createNamedQuery("AhTenantPluginCredential.findByTenantIdAndPluginName");
            query.setParameter("tenantId", tenantId);
            query.setParameter("pluginName", pluginName);

            List<AhTenantPluginCredential> list = query.getResultList();
            if (list != null && list.size() > 0) {
                tenant = list.get(0);

            }
        } finally {
            em.close();
        }
        return tenant;
    }

    public int getAhTenantCredentialCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<AhTenantPluginCredential> rt = cq.from(AhTenantPluginCredential.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }


}
