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
import org.openmrs.ConceptNumeric;
import org.openmrs.api.APIException;
import org.openmrs.module.openconceptlab.importer.ImportException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import static org.openmrs.module.openconceptlab.OpenConceptLabConstants.OPEN_CONCEPT_LAB_NAMESPACE_UUID;

/**
 * Contains most of the utility methods for Open Concept lab
 */
public class Utils {

	private static final DateFormat timeFormatter = new SimpleDateFormat("HH:mm");
	private static final DateFormat dateFormatter = new SimpleDateFormat("dd-MMM-yyyy");
	private static final int UUID_BYTE_LENGTH = 16;

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

	/**
	 * Implements a version 5 UUID from RFC 4122 using the {@link OpenConceptLabConstants#OPEN_CONCEPT_LAB_NAMESPACE_UUID}
	 * as the namespace.
	 *
	 * This ensures that user-visible UUIDs for imported data will remain consistent across imports
	 *
	 * @param name name to use to construct uuid; should be as unique as possible but predictable
	 * @return a {@link UUID} representing the intended UUID
	 */
	public static UUID version5Uuid(String name) {
		byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocate(UUID_BYTE_LENGTH + nameBytes.length);
		buffer.putLong(OPEN_CONCEPT_LAB_NAMESPACE_UUID.getMostSignificantBits());
		buffer.putLong(OPEN_CONCEPT_LAB_NAMESPACE_UUID.getLeastSignificantBits());
		buffer.put(nameBytes);

		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		}
		catch (NoSuchAlgorithmException e) {
			throw new APIException("Could not find an implementation of the SHA-1 algorithm", e);
		}

		byte[] digest = md.digest(buffer.array());

		byte[] uuidBytes = new byte[UUID_BYTE_LENGTH];
		// truncate the digest to 16 bytes
		System.arraycopy(digest, 0, uuidBytes, 0, UUID_BYTE_LENGTH);

		uuidBytes[6] &= 0x0f;   // clear the version
		uuidBytes[6] |= 5 << 4; // set the version to 5
		uuidBytes[8] &= 0x3f;   // clear the variant
		uuidBytes[8] |= 0x80;   // set the variant to IETF

		// convert byte array to UUID
		long msb = 0L;
		long lsb = 0L;

		for (int i = 0; i < UUID_BYTE_LENGTH / 2; i++) {
			msb = (msb << 8) | uuidBytes[i] & 0xff;
			lsb = (lsb << 8) | uuidBytes[i + 8] & 0xff;
		}

		return new UUID(msb, lsb);
	}

	public static void setAllowDecimal(ConceptNumeric numeric, Boolean allowDecimal) {
		try {
			Method setPrecise = numeric.getClass().getDeclaredMethod("setPrecise", Boolean.class);
			setPrecise.invoke(numeric, allowDecimal);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			try {
				Method setAllowDecimal = numeric.getClass().getDeclaredMethod("setAllowDecimal", Boolean.class);
				setAllowDecimal.invoke(numeric, allowDecimal);
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e1) {
				throw new ImportException(e1);
			}
		}
	}

	public static Boolean getAllowDecimal(ConceptNumeric numeric) {
		try {
			Method getPrecise = numeric.getClass().getDeclaredMethod("getPrecise");
			return (Boolean) getPrecise.invoke(numeric);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			try {
				Method getAllowDecimal = numeric.getClass().getDeclaredMethod("getAllowDecimal");
				return (Boolean) getAllowDecimal.invoke(numeric);
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e1) {
				throw new ImportException(e1);
			}
		}
	}

	public static String normalizeConceptSourceName(String name) {
		return name.toUpperCase()
				.replace(" ", "")
				.replace("-", "")
				.replace("_", "");
	}
}
