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

import java.security.MessageDigest;
import java.util.Comparator;
import java.util.Date;

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

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.openmrs.module.openconceptlab.client.OclConcept;
import org.openmrs.module.openconceptlab.client.OclMapping;

@Entity(name = "OclItem")
@Table(name = "openconceptlab_item")
public class Item {
	
	@Id
	@GeneratedValue
	@Column(name = "item_id")
	private Long itemId;
	
	@ManyToOne
	@JoinColumn(name = "import_id")
	private Import anImport;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "type")
	private ItemType type;
	
	@Basic
	@Column(name = "uuid")
	private String uuid;
	
	@Basic
	@Column(name = "url")
	private String url;
	
	@Basic
	@Column(name = "hashed_url")
	private byte[] hashedUrl;
	
	@Basic
	@Column(name = "version_url")
	private String versionUrl;
	
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "state")
	private ItemState state;
	
	@Basic
	@Column(name = "error_message", length = 1024)
	private String errorMessage;
	
	@Basic
	@Column(name = "updated_on")
	private Date updatedOn;
	
	public Item() {
		//for persistence only
	}
	
	public Item(Import anImport, OclConcept concept, ItemState state) {
		this.anImport = anImport;
		this.url = concept.getUrl();
		this.hashedUrl = hashUrl(url);
		this.versionUrl = concept.getVersionUrl();
		this.type = ItemType.CONCEPT;
		this.uuid = concept.getExternalId();
		this.state = state;
	}
	
	public Item(Import anImport, OclMapping oclMapping, ItemState state) {
		this(anImport, oclMapping, state, null);
	}
	
	public Item(Import anImport, OclMapping oclMapping, ItemState state, String errorMessage) {
		this.anImport = anImport;
		this.url = oclMapping.getUrl();
		this.hashedUrl = hashUrl(url);
		this.versionUrl = oclMapping.getUrl();
		this.type = ItemType.MAPPING;
		this.uuid = oclMapping.getExternalId();
		this.state = state;
		this.errorMessage = errorMessage;
		this.updatedOn = oclMapping.getUpdatedOn();
	}
	
	public static final byte[] hashUrl(String url) {
		if (url == null) {
			return null;
		}
		
		try {
			return MessageDigest.getInstance("MD5").digest(url.getBytes("UTF-8"));
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Cannot hash " + url, e);
		}
	}
	
	public Long getItemId() {
		return itemId;
	}
	
	public Import getAnImport() {
		return anImport;
	}
	
	public void setAnImport(Import anImport) {
		this.anImport = anImport;
	}
	
	public ItemType getType() {
		return type;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public String getUrl() {
		return url;
	}
	
	public byte[] getHashedUrl() {
		return hashedUrl;
	}
	
	public String getVersionUrl() {
		return versionUrl;
	}
	
	public ItemState getState() {
		return state;
	}
	
	public void setState(ItemState state) {
		this.state = state;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public Date getUpdatedOn() {
		return updatedOn;
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
