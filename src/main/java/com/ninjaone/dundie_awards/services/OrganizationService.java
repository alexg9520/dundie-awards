package com.ninjaone.dundie_awards.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.exceptions.LookupException;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/** Service class for managing organizations */
@Slf4j
@Component
public class OrganizationService extends AbstractDundieService {

    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Get organization info by id
     * 
     * @param organizationId the id of the organization
     * @return OrganizationInfo record
     * @throws LookupException if organization is not found
     * @throws InvalidArgumentException if organizationId is null
     */
    @Cacheable(value="organization", key="#organizationId")
    public OrganizationInfo getOrganizationInfo(Long organizationId) throws LookupException, InvalidArgumentException {
        checkForNullValue(organizationId, "Organization ID is null", "No organization ID was provided");
        return createOrganizationInfoFromOrganizationNoCheck(getOrganizationData(organizationId));
    }
    
    /**
     * Get organization entity by id with caching
     * 
     * @param organizationId the id of the organization
     * @return Organization entity
     * @throws LookupException if organization is not found
    */
    private Organization getOrganizationData(Long organizationId) throws LookupException {
        Optional<Organization> organization = organizationRepository.findById(organizationId);
        if (!organization.isPresent()) {
            LookupException lookupException = new LookupException("The organization was not found");
            log.error("Organization not found with ID: {}", organizationId, lookupException);
            throw lookupException;
        }
        return organization.get();
    }

    /**
     * Save a new organization
     * 
     * @param organization the OrganizationInfo to save
     * @return the saved OrganizationInfo record
     * @throws InvalidArgumentException if organization is null
     */
    public OrganizationInfo save(OrganizationInfo organization) throws InvalidArgumentException {
        checkForNullValue(organization, "OrganizationInfo is null", "No organization information was provided");
        Organization newOrganizationData = new Organization(organization.name());
        return createOrganizationInfoFromOrganization(saveData(newOrganizationData));
    }

    /**
     * Save organization data to the repository
     * 
     * @param newOrganizationData the Organization entity to save
     * @return the saved Organization entity
     */
    @Transactional
    private Organization saveData(Organization newOrganizationData) {
        return organizationRepository.save(newOrganizationData);
    }

    /**
     * Delete organization by id. Should only be called if there are no employees in the organization.
     * 
     * @param id the id of the organization to delete
     * @return OrganizationInfo of deleted organization
     * @throws LookupException if organization is not found
     * @throws InvalidArgumentException if id is null
     */
    @Transactional
    protected OrganizationInfo delete(Long id) throws LookupException, InvalidArgumentException {
        Organization organizationData = getOrganizationData(id);
        OrganizationInfo organizationInfo = createOrganizationInfoFromOrganizationNoCheck(organizationData);
        organizationRepository.delete(organizationData);

        // Evict caches since organization data may have changed
        evictCachesAfterUpdate(organizationData.getId());

        return organizationInfo;
    }

    /**
     * Update organization by id
     * 
     * @param id the id of the organization to update
     * @param organizationInfo the new organization details
     * @return OrganizationInfo record of updated organization
     * @throws LookupException if organization is not found
     * @throws InvalidArgumentException if id or organizationInfo is null
     */
    @Transactional
    public OrganizationInfo update(Long id, OrganizationInfo organizationInfo) throws LookupException, InvalidArgumentException {
        checkForNullValue(id, "Organization ID is null", "No organization ID was provided");
        checkForNullValue(organizationInfo, "OrganizationInfo is null", "No organization information was provided");
        Organization organizationData = getOrganizationData(id);

        // Only allow updates to name
        organizationData.setName(organizationInfo.name());
        Organization updatedOrganization = organizationRepository.save(organizationData);

        // Evict caches since organization data may have changed
        evictCachesAfterUpdate(updatedOrganization.getId());

        return createOrganizationInfoFromOrganizationNoCheck(updatedOrganization);
    }

    /** Evict caches after updates to organization
     * 
     * @param organizationId the id of the organization
     */
    private void evictCachesAfterUpdate(Long organizationId) {
        evictOrganizationCache(organizationId);
    }

    /** Evict organization cache
     * 
     * @param organizationId the id of the organization
     */
    @CacheEvict(value="organization", key="#organizationId")
    private void evictOrganizationCache(long organizationId) {
        log.debug("Organization cache evicted for organizationId: {}", organizationId);
    }


}
