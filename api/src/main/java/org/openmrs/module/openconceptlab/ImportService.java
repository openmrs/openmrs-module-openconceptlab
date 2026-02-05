package org.openmrs.module.openconceptlab;

import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.annotation.Authorized;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;

public interface ImportService {

	/**
	 * @should return all updates ordered descending by ids
	 */
    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	List<Import> getImportsInOrder(int first, int max);

	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	List<Import> getInProgressImports();

    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	List<Concept> getConceptsByName(String name, Locale locale);

	/**
	 * @should return update with id
	 * @should throw IllegalArgumentException if update does not exist
	 */
    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
    Import getImport(Long id);

	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
    Import getImport(String uuid);

    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
    Import getLastImport();

    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
    Import getLastSuccessfulSubscriptionImport();
    
    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
    Boolean isLastImportSuccessful();

    @Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void ignoreAllErrors(Import update);

    @Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void failImport(Import update);

    @Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void failImport(Import update, String errorMessage);

	/**
	 * @should throw IllegalStateException if another update is in progress
	 */
    @Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void startImport(Import update);

    @Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void updateOclDateStarted(Import update, Date oclDateStarted);

	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void updateReleaseVersion(Import anImport, String version);

	/**
	 * @should throw IllegalArgumentException if not scheduled
	 * @should throw IllegalStateException if trying to stop twice
	 */
    @Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void stopImport(Import update);

    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	Item getLastSuccessfulItemByUrl(String url);

	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	Item getLastSuccessfulItemByUrl(String url, CacheService cacheService);

    @Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void saveItem(Item item);

    @Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void saveItems(Iterable<? extends Item> items);

	@Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	Item getItem(String uuid);

    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	Subscription getSubscription();

    @Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void saveSubscription(Subscription subscription);

    @Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void unsubscribe();

	/**
	 * @param update the update to be passed
	 * @param first starting index
	 * @param max maximum limit
	 * @return a list of items
	 */
    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	List<Item> getImportItems(Import update, int first, int max, Set<ItemState> states);

	/**
	 * @param update the update to be passed
	 * @param states set of states passed
	 * @return a count of items
	 */
    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	Integer getImportItemsCount(Import update, Set<ItemState> states);

	/**
	 * @param uuid the uuid to search a concept with
	 * @return true if subscribed else false
	 */
    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	Boolean isSubscribedConcept(String uuid);

    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	ConceptMap getConceptMapByUuid(String uuid);

    @Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	Concept updateConceptWithoutValidation(Concept concept);

    @Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	ConceptReferenceTerm updateConceptReferenceTermWithoutValidation(ConceptReferenceTerm term);

    /**
     * @param concept
     * @return
     * @should find duplicates
     */
    @Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	List<ConceptName> changeDuplicateConceptNamesToIndexTerms(Concept concept);

	@Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void updateSubscriptionUrl(Import anImport, String url);

	@Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	<T> T runInTransaction(Callable<T> callable) throws Exception;

	/**
	 * Flushes pending changes to the database and clears the Hibernate session cache.
	 * This prevents memory buildup during large imports by releasing cached entities.
	 */
	@Transactional
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	void flushAndClearSession();
}
