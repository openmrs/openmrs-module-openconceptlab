package org.openmrs.module.openconceptlab;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.springframework.transaction.annotation.Transactional;

public interface UpdateService {

	/**
	 * @should return all updates ordered descending by ids
	 */
    @Transactional(readOnly = true)
	List<Update> getUpdatesInOrder(int first, int max);

    @Transactional(readOnly = true)
	List<Concept> getConceptsByName(String name, Locale locale);

	/**
	 * @should return update with id
	 * @should throw IllegalArgumentException if update does not exist
	 */
    @Transactional(readOnly = true)
	Update getUpdate(Long id);

    @Transactional(readOnly = true)
	Update getLastUpdate();

    @Transactional(readOnly = true)
	Update getLastSuccessfulSubscriptionUpdate();

    @Transactional
	void ignoreAllErrors(Update update);

    @Transactional
	void failUpdate(Update update);

    @Transactional
	void failUpdate(Update update, String errorMessage);

	/**
	 * @should throw IllegalStateException if another update is in progress
	 */
    @Transactional
	void startUpdate(Update update);

    @Transactional
	void updateOclDateStarted(Update update, Date oclDateStarted);

	/**
	 * @should throw IllegalArgumentException if not scheduled
	 * @should throw IllegalStateException if trying to stop twice
	 */
    @Transactional
	void stopUpdate(Update update);

    @Transactional(readOnly = true)
	Item getLastSuccessfulItemByUrl(String url);

    @Transactional
	void saveItem(Item item);

    @Transactional
	void saveItems(Iterable<? extends Item> items);

    @Transactional(readOnly = true)
	Subscription getSubscription();

    @Transactional
	void saveSubscription(Subscription subscription);

    @Transactional
	void unsubscribe();

	/**
	 * @param update the update to be passed
	 * @param first starting index
	 * @param max maximum limit
	 * @return a list of items
	 */
    @Transactional(readOnly = true)
	List<Item> getUpdateItems(Update update, int first, int max, Set<ItemState> states);

	/**
	 * @param update the update to be passed
	 * @param states set of states passed
	 * @return a count of items
	 */
    @Transactional(readOnly = true)
	Integer getUpdateItemsCount(Update update, Set<ItemState> states);

	/**
	 * @param uuid the uuid to search a concept with
	 * @return true if subscribed else false
	 */
    @Transactional(readOnly = true)
	Boolean isSubscribedConcept(String uuid);

    @Transactional(readOnly = true)
	ConceptMap getConceptMapByUuid(String uuid);

    @Transactional
	Concept updateConceptWithoutValidation(Concept concept);

    @Transactional
	ConceptReferenceTerm updateConceptReferenceTermWithoutValidation(ConceptReferenceTerm term);

    /**
     * @param concept
     * @return
     * @should find duplicates
     */
    @Transactional(readOnly = true)
	List<ConceptName> changeDuplicateConceptNamesToIndexTerms(Concept concept);

}
