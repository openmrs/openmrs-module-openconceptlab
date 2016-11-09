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

import org.apache.commons.lang3.time.DateUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

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
		if(date == null) {
			date = new Date();
		}
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

	public static InputStream extractExportInputStreamFromZip(ZipFile zipfile) throws IOException {

		//todo overload method with filename? is export.json ok?
		final String fileToBeExtracted="export.json";

		InputStream in;
		try {
			in = zipfile.getInputStream(zipfile.getEntry(fileToBeExtracted));
		}
		catch (IOException e) {
			throw new IOException("Failed to load " + fileToBeExtracted + " from " + zipfile.getName(), e);
		}

		return in;
	}

}
