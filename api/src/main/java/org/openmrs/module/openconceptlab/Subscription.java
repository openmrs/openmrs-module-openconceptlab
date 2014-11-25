package org.openmrs.module.openconceptlab;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Subscription {
	
	private String url;
	
	private String token;
	
	private Integer days = 0;
	
	private Integer hours = 0;
	
	private Integer minutes = 0;
	
	public Subscription() {
		
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
