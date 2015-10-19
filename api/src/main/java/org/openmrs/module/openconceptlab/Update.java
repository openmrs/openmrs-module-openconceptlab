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

import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

@Entity(name = "OclUpdate")
@Table(name = "openconceptlab_update")
public class Update {
	
	@Id
	@GeneratedValue
	@Column(name = "update_id")
	private Long updateId;
	
	@Basic
	@Column(name = "local_date_started")
	private Date localDateStarted = new Date();
	
	@Basic
	@Column(name = "local_date_stopped")
	private Date localDateStopped;
	
	@Basic
	@Column(name = "ocl_date_started")
	private Date oclDateStarted;
	
	@Basic
	@Column(name = "error_message")
	private String errorMessage;
	
	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "update_id")
	@Sort(type = SortType.COMPARATOR, comparator = Item.OrderByState.class)
	private SortedSet<Item> items = new TreeSet<Item>(new Item.OrderByState());
	
	public Long getUpdateId() {
		return updateId;
	}
	
	public Date getLocalDateStarted() {
		return localDateStarted;
	}
	
	public Date getLocalDateStopped() {
		return localDateStopped;
	}
	
	void stop() {
		localDateStopped = new Date();
	}
	
	public Date getOclDateStarted() {
		return oclDateStarted;
	}
	
	public void setOclDateStarted(Date oclDateStarted) {
		this.oclDateStarted = oclDateStarted;
	}
	
    public String getErrorMessage() {
	    return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
	    this.errorMessage = errorMessage;
    }
	
	public SortedSet<Item> getItems() {
		return items;
	}
	
	public boolean isStopped() {
		return localDateStopped != null;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(updateId).build();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		Update other = (Update) obj;
		return new EqualsBuilder().append(getUpdateId(), other.getUpdateId()).build();
	}
}
