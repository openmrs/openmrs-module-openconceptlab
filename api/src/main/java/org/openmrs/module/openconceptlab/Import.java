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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity(name = "OclImport")
@Table(name = "openconceptlab_import")
public class Import {
	
	@Id
	@GeneratedValue
	@Column(name = "import_id")
	private Long importId;

	@Basic
	@Column(name = "uuid")
	private String uuid = UUID.randomUUID().toString();

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
	@Column(name = "release_version")
	private String releaseVersion;
	
	@Basic
	@Column(name = "error_message")
	private String errorMessage;
	@Basic
	@Column(name = "subscription_url")
	private String subscriptionUrl;

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Long getImportId() {
		return importId;
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

	public String getReleaseVersion() {
		return releaseVersion;
	}

	public void setReleaseVersion(String releaseVersion) {
		this.releaseVersion = releaseVersion;
	}
	
    public String getErrorMessage() {
	    return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
	    this.errorMessage = errorMessage;
    }
	
	public boolean isStopped() {
		return localDateStopped != null;
	}

	public String getSubscriptionUrl() {
		return subscriptionUrl;
	}

	public void setSubscriptionUrl(String subscriptionUrl) {
		this.subscriptionUrl = subscriptionUrl;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(importId).build();
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
		Import other = (Import) obj;
		return new EqualsBuilder().append(getImportId(), other.getImportId()).build();
	}
}
