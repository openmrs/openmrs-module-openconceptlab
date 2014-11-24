package org.openmrs.module.openconceptlab.updater;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmrs.module.openconceptlab.Item;
import org.openmrs.module.openconceptlab.MockTest;
import org.openmrs.module.openconceptlab.State;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.TestResources;
import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclClient.OclResponse;
import org.openmrs.module.openconceptlab.updater.ImportQueue;
import org.openmrs.module.openconceptlab.updater.Importer;
import org.openmrs.module.openconceptlab.updater.Updater;

public class UpdaterTest extends MockTest {
	
	@Mock
	OclClient oclClient;
	
	@Mock
	UpdateService updateService;
	
	@Mock
	Importer importer;
	
	@InjectMocks
	Updater updater;
	
	/**
	 * @see Updater#run()
	 * @verifies start first update with response date
	 */
	@Test
	public void runUpdate_shouldStartFirstUpdateWithResponseDate() throws Exception {
		Subscription subscription = new Subscription();
		subscription.setUrl("http://some.com/url");
		when(updateService.getSubscription()).thenReturn(subscription);
		
		Date updatedTo = new Date();
		OclResponse oclResponse = new OclClient.OclResponse(IOUtils.toInputStream("{}"), 0, updatedTo);
		when(updateService.getLastUpdate()).thenReturn(null);
		when(oclClient.fetchUpdates(subscription.getUrl(), null)).thenReturn(oclResponse);
		
		updater.run();
		
		verify(updateService).startUpdate(argThat(hasOclDateStarted(updatedTo)));
	}
	
	/**
	 * @see Updater#run()
	 * @verifies start next update with updated since
	 */
	@Test
	public void runUpdate_shouldStartNextUpdateWithUpdatedSince() throws Exception {
		Subscription subscription = new Subscription();
		subscription.setUrl("http://some.com/url");
		when(updateService.getSubscription()).thenReturn(subscription);
		
		Update lastUpdate = new Update();
		Date updatedSince = new Date();
		lastUpdate.setOclDateStarted(updatedSince);
		when(updateService.getLastUpdate()).thenReturn(lastUpdate);
		
		Date updatedTo = new Date();
		OclResponse oclResponse = new OclClient.OclResponse(IOUtils.toInputStream("{}"), 0, updatedTo);
		when(oclClient.fetchUpdates(subscription.getUrl(), lastUpdate.getOclDateStarted())).thenReturn(oclResponse);
		
		updater.run();
		
		verify(updateService).startUpdate(argThat(hasOclDateStarted(updatedTo)));
	}
	
	/**
	 * @see Updater#run()
	 * @verifies create item for each concept
	 */
	@Test
	public void runUpdate_shouldCreateItemForEachConcept() throws Exception {
		Subscription subscription = new Subscription();
		subscription.setUrl("http://some.com/url");
		when(updateService.getSubscription()).thenReturn(subscription);
		
		Update lastUpdate = new Update();
		Date updatedSince = new Date();
		lastUpdate.setOclDateStarted(updatedSince);
		when(updateService.getLastUpdate()).thenReturn(lastUpdate);
		
		Date updatedTo = new Date();
		OclResponse oclResponse = new OclClient().unzipResponse(TestResources.getSimpleResponseAsStream(), updatedTo);
		when(oclClient.fetchUpdates(subscription.getUrl(), lastUpdate.getOclDateStarted())).thenReturn(oclResponse);
		doAnswer(new Answer<Item>() {

			@Override
            public Item answer(InvocationOnMock invocation) throws Throwable {
				Update update = (Update) invocation.getArguments()[0];
				ImportQueue importQueue = (ImportQueue) invocation.getArguments()[1];
				OclConcept oclConcept = importQueue.poll();
	            return new Item(update, oclConcept, State.ADDED);
            }}).when(importer).importConcept(any(Update.class), any(ImportQueue.class));
		
		
		updater.run();
		
		verify(updateService).saveItem(argThat(hasUuid("5435b10b50d61b61c48ec449")));
		verify(updateService).saveItem(argThat(hasUuid("543583f750d61b5bfd7df26f")));
		verify(updateService).saveItem(argThat(hasUuid("54348e1150d61b2a914bdd01")));
	}
	
	public Matcher<Update> hasOclDateStarted(Date oclDateStarted) {
		return new FeatureMatcher<Update, Date>(
		                                        is(oclDateStarted), "oclDateStarted", "oclDateStarted") {
			
			@Override
			protected Date featureValueOf(Update actual) {
				return actual.getOclDateStarted();
			}
		};
	}
	
	public Matcher<Item> hasUuid(String uuid) {
		return new FeatureMatcher<Item, String>(
		                                        is(uuid), "uuid", "uuid") {
			
			@Override
			protected String featureValueOf(Item actual) {
				return actual.getUuid();
			}
		};
	}
}
