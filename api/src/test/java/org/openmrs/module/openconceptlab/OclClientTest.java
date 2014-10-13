package org.openmrs.module.openconceptlab;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.DateUtil;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.module.openconceptlab.OclClient.OclResponse;

public class OclClientTest extends MockTest {
	
	OclClient oclClient = new OclClient();
	
	@Mock
	GetMethod get;
	
	/**
	 * @see OclClient#extractResponse(GetMethod)
	 * @verifies extract date and json
	 */
	@Test
	public void extractResponse_shouldExtractDateAndJson() throws Exception {
		String date = "Mon, 13 Oct 2014 11:07:19 GMT";
		Header header = new Header("Date", date);
		
		when(get.getResponseHeader("date")).thenReturn(header);
		when(get.getResponseBodyAsStream()).thenReturn(getClass().getClassLoader().getResourceAsStream("response.zip"));
		
		OclResponse subscription = oclClient.extractResponse(get);
		
		assertThat(subscription.getUpdatedTo(), is(DateUtil.parseDate(date)));
		assertThat(subscription.getJson(), startsWith("[{\"type\": \"Concept\", \"uuid\": \"5435b10b50d61b61c48ec449\""));
		assertThat(subscription.getJson().length(), is(3401));
	}
}
