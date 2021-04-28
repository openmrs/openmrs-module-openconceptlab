/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab.client;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OclMapping {
	
	private String id;
	
	@JsonProperty("external_id")
	private String externalId;
	
	private String type;
	
	private String url;
	
	private Boolean retired;
	
	@JsonProperty("map_type")
	private String mapType;
	
	@JsonProperty("from_source_url")
	private String fromSourceUrl;
	
	@JsonProperty("from_concept_url")
	private String fromConceptUrl;
	
	@JsonProperty("to_concept_url")
	private String toConceptUrl;
	
	@JsonProperty("to_source_name_resolved")
	private String toSourceName;
	
	@JsonProperty("to_concept_code")
	private String toConceptCode;
	
	@JsonProperty("updated_on")
	private Date updatedOn;
	
	public static abstract class MapType {
		
		public static final String Q_AND_A = "Q-AND-A";
		
		public static final String SET = "CONCEPT-SET";
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getExternalId() {
		return externalId;
	}
	
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public Boolean getRetired() {
		return retired;
	}
	
	public boolean isRetired() {
		return Boolean.TRUE.equals(retired);
	}
	
	public void setRetired(Boolean retired) {
		this.retired = retired;
	}
	
	public String getMapType() {
		return mapType;
	}
	
	public void setMapType(String mapType) {
		this.mapType = mapType;
	}
	
	public String getFromSourceUrl() {
		return fromSourceUrl;
	}
	
	public void setFromSourceUrl(String fromSourceUrl) {
		this.fromSourceUrl = fromSourceUrl;
	}
	
	public String getFromConceptUrl() {
		return fromConceptUrl;
	}
	
	public void setFromConceptUrl(String fromConceptUrl) {
		this.fromConceptUrl = fromConceptUrl;
	}
	
	public String getToSourceName() {
		return toSourceName != null ? toSourceName : toSourceNameResolved;
	}
	
	public void setToSourceName(String toSourceName) {
		this.toSourceName = toSourceName;
	}
	
	public String getToConceptCode() {
		return toConceptCode;
	}
	
	public void setToConceptCode(String toConceptCode) {
		this.toConceptCode = toConceptCode;
	}
	
	public String getToConceptUrl() {
		return toConceptUrl;
	}
	
	public void setToConceptUrl(String toConceptUrl) {
		this.toConceptUrl = toConceptUrl;
	}

	public Date getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Date updatedOn) {
		this.updatedOn = updatedOn;
	}

	@Override
	public String toString() {
	    return new ToStringBuilder(this).append("externalId", externalId).build();
	}
}
