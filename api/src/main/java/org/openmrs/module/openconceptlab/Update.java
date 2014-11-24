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

import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

@Entity
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
	
	@OneToMany(mappedBy = "update", fetch = FetchType.LAZY)
	@OrderBy("state DESC")
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
