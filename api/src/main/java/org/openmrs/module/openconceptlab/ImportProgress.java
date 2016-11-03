/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab;

import java.util.concurrent.TimeUnit;

/**
 * Class to show the update progress
 */
public class ImportProgress {

	private Integer progress;
	private Long time;
	private String timeText;

	public Integer getProgress() {
		return progress;
	}

	public void setProgress(Integer progress) {
		this.progress = progress;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
		
		timeText = convertSecondsToText(time);
	}

	public static String convertSecondsToText(Long time) {
		if (time < 60) {
			return time + " seconds";
		} else {
		    long hours = TimeUnit.HOURS.convert(time, TimeUnit.SECONDS);
		    long minutes = TimeUnit.MINUTES.convert(time, TimeUnit.SECONDS) - TimeUnit.MINUTES.convert(hours, TimeUnit.HOURS);
		    long seconds = time - TimeUnit.SECONDS.convert(hours, TimeUnit.HOURS) - TimeUnit.SECONDS.convert(minutes, TimeUnit.MINUTES);
		    return ((hours > 0) ? hours + " hours " : "") + ((minutes > 0) ? minutes + " minutes " : "") + seconds + " seconds";
		}
    }
	
    public String getTimeText() {
	    return timeText;
    }
}
