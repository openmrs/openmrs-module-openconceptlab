package org.openmrs.module.openconceptlab;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Subscription {
	
	private String url;
	
	private Integer days;
	
	private Integer hours;
	
	private Integer minutes;
	
	public Subscription() {
		
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
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
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(url).append(days).append(hours).append(minutes).build();
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
		return new EqualsBuilder().append(other.url, url).append(other.days, days).append(other.hours, hours)
		        .append(other.minutes, minutes).build();
	}
	
}
