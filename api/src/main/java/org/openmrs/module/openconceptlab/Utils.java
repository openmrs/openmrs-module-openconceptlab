/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.openconceptlab;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Contains most of the utility methods for Open Concept lab
 */
public class Utils {

	/**
	 * Add days to an existing date
	 * @param date the date
	 * @param days the number of days to add (negative to subtract days)
	 * @return the new date
	 * @should shift the date by the number of days
	 */
	public static Date dateAddDays(Date date, int days) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DATE, days);
		return cal.getTime();
	}

	/**
	 * format date into my locale
	 */
	public static String formatedDate(Date date) {
		DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.ENGLISH);
		return dateFormatter.format(date);
	}

	/**
	 * Difference between two dates
	 * @return duration
	 */
	public static Long dateDifference(Date date1, Date date2, TimeUnit timeUnit){
		long diffInMillies = date2.getTime() - date1.getTime();
		return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
	}

	/**
	 * Extracting time from date
	 * @return time upgrade started
	 */
	public static String timeFromDateTime(Date date) {
		DateFormat dateFormatter = DateFormat.getTimeInstance(DateFormat.DEFAULT, Locale.ENGLISH);
		return dateFormatter.format(date);
	}
}
