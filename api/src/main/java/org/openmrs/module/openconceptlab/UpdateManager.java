package org.openmrs.module.openconceptlab;

import java.io.IOException;
import java.util.Date;

import org.openmrs.module.openconceptlab.OclClient.OclResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateManager {
	
	@Autowired
	UpdateService updateService;
	
	@Autowired
	OclClient oclClient;
		
	/**
	 * @should start first update with response date
	 * @should start next update with updated since
	 */
	public void runUpdate() throws IOException {
		Subscription subscription = updateService.getSubscription();
		Update lastUpdate = updateService.getLastUpdate();
		Date updatedSince = null;
		if (lastUpdate != null) {
			updatedSince = lastUpdate.getOclDateStarted();
		}
		
		OclResponse oclResponse = oclClient.fetchUpdates(subscription.getUrl(), updatedSince);
		
		Update update = new Update();
		update.setOclDateStarted(oclResponse.getUpdatedTo());
		updateService.startUpdate(update);
		updateService.stopUpdate(update);
	}
}
