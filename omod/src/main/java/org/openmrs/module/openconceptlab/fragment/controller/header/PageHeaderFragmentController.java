package org.openmrs.module.openconceptlab.fragment.controller.header;

import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.openconceptlab.util.ServerInformation;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

import java.util.Date;
import java.util.Map;

/**
* Controller for page header
*/
public class PageHeaderFragmentController {

	public void controller(FragmentModel model,
						   @SpringBean KenyaUiUtils kenyaui) {

		Map<String, Object> openConceptLabInfo = ServerInformation.getopenConceptLabInformation();

		String moduleVersion = (String) openConceptLabInfo.get("version");
		boolean isSnapshot = moduleVersion.endsWith("SNAPSHOT");

		if (isSnapshot) {
			Date moduleBuildDate = (Date) openConceptLabInfo.get("buildDate");
			moduleVersion += " (" + kenyaui.formatDateTime(moduleBuildDate) + ")";
		}

		model.addAttribute("moduleVersion", moduleVersion);
	}
}
