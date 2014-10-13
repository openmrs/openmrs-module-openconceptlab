package org.openmrs.module.openconceptlab;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.module.openconceptlab.OclClient.OclResponse;

public class UpdateManagerTest extends MockTest {
	
	@Mock
	OclClient oclClient;
	
	@Mock
	UpdateService updateService;
	
	@InjectMocks
	UpdateManager updateManager;
	
	/**
	 * @see UpdateManager#runUpdate()
	 * @verifies start first update with response date
	 */
	@Test
	public void runUpdate_shouldStartFirstUpdateWithResponseDate() throws Exception {
		Subscription subscription = new Subscription();
		subscription.setUrl("http://some.com/url");
		when(updateService.getSubscription()).thenReturn(subscription);
		
		Date updatedTo = new Date();
		OclResponse oclResponse = new OclClient.OclResponse("", updatedTo);
		when(updateService.getLastUpdate()).thenReturn(null);
		when(oclClient.fetchUpdates(subscription.getUrl(), null)).thenReturn(oclResponse);
		
		updateManager.runUpdate();
		
		verify(updateService).startUpdate(argThat(hasOclDateStarted(updatedTo)));
		;
	}
	
	/**
	 * @see UpdateManager#runUpdate()
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
		OclResponse oclResponse = new OclClient.OclResponse("", updatedTo);
		when(oclClient.fetchUpdates(subscription.getUrl(), lastUpdate.getOclDateStarted())).thenReturn(oclResponse);
		
		updateManager.runUpdate();
		
		verify(updateService).startUpdate(argThat(hasOclDateStarted(updatedTo)));
		;
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
}
