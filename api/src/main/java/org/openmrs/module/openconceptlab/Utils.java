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

import org.apache.commons.lang3.time.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Contains most of the utility methods for Open Concept lab
 */
public class Utils {

	private static final DateFormat timeFormatter = new SimpleDateFormat("HH:mm");
	private static final DateFormat dateFormatter = new SimpleDateFormat("dd-MMM-yyyy");

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
	 * Formats a date, automatically inferring the best format
	 * @param date the date
	 * @return the string value
	 */
	public static String formatDateAuto(Date date) {
		 if (DateUtils.isSameDay(date, new Date())) {
			return "Today at "+formatTime(date);
		}
		 else if(DateUtils.isSameDay(date, dateAddDays(new Date(), -1))) {
			return "Yesterday at "+formatTime(date);
		}
		 else {
			return formatDateTime(date);
		}
	}

	/**
	 * Formats a date as a time value only
	 * @param date the date
	 * @return the string value
	 * @should format date as a string without time information
	 * @should format null date as empty string
	 */
	public static String formatTime(Date date) {
		if (date == null) {
			return "";
		}

		return timeFormatter.format(date);
	}

	/**
	 * Formats a date time
	 * @param date the date
	 * @return the string value
	 */
	public static String formatDateTime(Date date) {
		if (date == null) {
			return "";
		}

		return dateFormatter.format(date) + " " + timeFormatter.format(date);
	}
}
