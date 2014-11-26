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

import java.util.Comparator;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.openmrs.module.openconceptlab.client.OclConcept;

@Entity(name = "OclItem")
@Table(name = "openconceptlab_item")
public class Item {
	
	@Id
	@GeneratedValue
	@Column(name = "item_id")
	private Long itemId;
	
	@ManyToOne
	@JoinColumn(name = "update_id")
	private Update update;
	
	@Basic
	@Column(name = "type")
	private String type;
	
	@Basic
	@Column(name = "uuid")
	private String uuid;
	
	@Basic
	@Column(name = "version_url")
	private String versionUrl;
	
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "state")
	private State state;
	
	@Basic
	@Column(name = "error_message")
	private String errorMessage;
	
	protected Item() {
		//for persistence only
	}
	
	public Item(Update update, OclConcept concept, State state) {
		this.update = update;
		this.versionUrl = concept.getVersionUrl();
		if (StringUtils.isBlank(concept.getType())) {
			this.type = "Concept";
		} else {
			this.type = concept.getType();
		}
		this.uuid = concept.getUuid();
		this.state = state;
	}
	
	public Long getItemId() {
		return itemId;
	}
	
	public Update getUpdate() {
		return update;
	}
	
	public String getType() {
		return type;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public String getVersionUrl() {
		return versionUrl;
	}
	
	public State getState() {
		return state;
	}
	
	public void setState(State state) {
		this.state = state;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public static class OrderByState implements Comparator<Item> {
		
		@Override
		public int compare(Item o1, Item o2) {
			return new CompareToBuilder().append(o2.getState(), o1.getState()).append(o2.getItemId(), o1.getItemId())
			        .build();
		}
		
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(getItemId()).build();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Item other = (Item) obj;
		return new EqualsBuilder().append(getItemId(), other.getItemId()).build();
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("itemId", itemId).append("uuid", uuid).build();
	}
	
}
