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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Subscription {

	private String uuid;

	private String url;

	private String token;

	private Integer days = 0;

	private Integer hours = 0;

	private Integer minutes = 0;

	private boolean subscribedToSnapshot = true;

	public Subscription() {

	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getToken() {
		return token;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
	
	public Integer getDays() {
		return days;
	}
	
	public void setDays(Integer days) {
		this.days = days;
	}
	
	public Integer getHours() {
		return hours;
	}
	
	public void setHours(Integer hours) {
		this.hours = hours;
	}
	
	public Integer getMinutes() {
		return minutes;
	}
	
	public void setMinutes(Integer minutes) {
		this.minutes = minutes;
	}
	
	public boolean isManual() {
		return (days == null && hours == null && minutes == null) || (days == 0 && hours == 0 && minutes == 0);
	}

	public boolean isSubscribedToSnapshot() {
		return subscribedToSnapshot;
	}

	public void setSubscribedToSnapshot(boolean isFetchingSnapshotUpdates) {
		this.subscribedToSnapshot = isFetchingSnapshotUpdates;
	}
	
	public boolean isSubscribed() {
		return !StringUtils.isBlank(url);
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(url).append(token).append(days).append(hours).append(minutes).build();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subscription other = (Subscription) obj;
		return new EqualsBuilder().append(other.url, url).append(other.token, token).append(other.days, days)
		        .append(other.hours, hours).append(other.minutes, minutes).build();
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("url", url).append("token", token).append("days", days)
		        .append("hours", hours).append("minutes", minutes).build();
	}
	
}
