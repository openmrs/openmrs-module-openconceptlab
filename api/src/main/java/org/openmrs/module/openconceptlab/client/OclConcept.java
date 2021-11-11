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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OclConcept {

	private String type;

	private String uuid;

	private String id;

	@JsonProperty("external_id")
	private String externalId;

	@JsonProperty("concept_class")
	private String conceptClass;

	private String datatype;

	private List<Name> names = new ArrayList<>();

	private List<Description> descriptions = new ArrayList<>();

	private boolean retired;

	private String url;

	@JsonProperty("version_url")
	private String versionUrl;

	private String source;

	@JsonProperty("source_url")
	private String sourceUrl;

	@JsonProperty("created_on")
	private Date dateCreated;

	@JsonProperty("updated_on")
	private Date dateUpdated;

	private Extras extras;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
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

	public String getConceptClass() {
		return conceptClass;
	}

	public void setConceptClass(String conceptClass) {
		this.conceptClass = conceptClass;
	}

	public String getDatatype() {
		return datatype;
	}

	public void setDatatype(String datatype) {
		this.datatype = datatype;
	}

	public List<Name> getNames() {
		return names;
	}

	public void setNames(List<Name> names) {
		this.names = names;
	}

	public List<Description> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(List<Description> descriptions) {
		this.descriptions = descriptions;
	}

	public boolean isRetired() {
		return retired;
	}

	public void setRetired(boolean retired) {
		this.retired = retired;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getVersionUrl() {
		return versionUrl;
	}

	public void setVersionUrl(String versionUrl) {
		this.versionUrl = versionUrl;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public Date getDateUpdated() {
		return dateUpdated;
	}

	public void setDateUpdated(Date dateUpdated) {
		this.dateUpdated = dateUpdated;
	}

	public Extras getExtras() {
		return extras;
	}

	public void setExtras(Extras extras) {
		this.extras = extras;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Name {

		private String uuid;

		@JsonProperty("external_id")
		private String externalId;

		private Locale locale;

		@JsonProperty("locale_preferred")
		private boolean localePreferred;

		private String name;

		@JsonProperty("name_type")
		private String nameType;

		private String type;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public String getExternalId() {
			return externalId;
		}

		public void setExternalId(String externalId) {
			this.externalId = externalId;
		}

		public Locale getLocale() {
			return locale;
		}

		public void setLocale(Locale locale) {
			this.locale = locale;
		}

		public boolean isLocalePreferred() {
			return localePreferred;
		}

		public void setLocalePreferred(boolean localePreferred) {
			this.localePreferred = localePreferred;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getNameType() {
			return nameType;
		}

		public void setNameType(String nameType) {
			this.nameType = nameType;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this).append("name", name).build();
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(locale).append(localePreferred).append(name).append(nameType).build();
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
			Name rhs = (Name) obj;
			return new EqualsBuilder().append(locale, rhs.locale).append(localePreferred, rhs.localePreferred)
			        .append(name, rhs.name).append(nameType, rhs.nameType).build();
		}

		public void copyFrom(ConceptName name) {
			this.name = name.getName();
			locale = name.getLocale();
			localePreferred = name.getLocalePreferred() != null ? name.getLocalePreferred() : false;
			nameType = name.getConceptNameType() != null ? name.getConceptNameType().toString() : null;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Description {

		private String uuid;

		@JsonProperty("external_id")
		private String externalId;

		private Locale locale;

		private String description;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public String getExternalId() {
			return externalId;
		}

		public void setExternalId(String externalId) {
			this.externalId = externalId;
		}

		public Locale getLocale() {
			return locale;
		}

		public void setLocale(Locale locale) {
			this.locale = locale;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public void copyFrom(ConceptDescription description) {
			this.description = description.getDescription();
			locale = description.getLocale();
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(locale).append(description).build();
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
			Description rhs = (Description) obj;
			return new EqualsBuilder().append(locale, rhs.locale).append(description, rhs.description).build();
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this).append("description", description).append("locale", locale).build();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Extras {

		@JsonProperty("hi_absolute")
		private Double hiAbsolute;

		@JsonProperty("hi_critical")
		private Double hiCritical;

		@JsonProperty("hi_normal")
		private Double hiNormal;

		@JsonProperty("low_absolute")
		private Double lowAbsolute;

		@JsonProperty("low_critical")
		private Double lowCritical;

		@JsonProperty("low_normal")
		private Double lowNormal;

		private String units;

		@Deprecated
		private Boolean precise;

		@JsonProperty("allow_decimal")
		private Boolean allowDecimal;

		public Double getHiAbsolute() {
			return hiAbsolute;
		}

		public void setHiAbsolute(Double hiAbsolute) {
			this.hiAbsolute = hiAbsolute;
		}

		public Double getHiCritical() {
			return hiCritical;
		}

		public void setHiCritical(Double hiCritical) {
			this.hiCritical = hiCritical;
		}

		public Double getHiNormal() {
			return hiNormal;
		}

		public void setHiNormal(Double hiNormal) {
			this.hiNormal = hiNormal;
		}

		public Double getLowAbsolute() {
			return lowAbsolute;
		}

		public void setLowAbsolute(Double lowAbsolute) {
			this.lowAbsolute = lowAbsolute;
		}

		public Double getLowCritical() {
			return lowCritical;
		}

		public void setLowCritical(Double lowCritical) {
			this.lowCritical = lowCritical;
		}

		public Double getLowNormal() {
			return lowNormal;
		}

		public void setLowNormal(Double lowNormal) {
			this.lowNormal = lowNormal;
		}

		public String getUnits() {
			return units;
		}

		public void setUnits(String units) {
			this.units = units;
		}

		@Deprecated
		public Boolean getPrecise() {
			return precise;
		}

		@Deprecated
		public void setPrecise(Boolean precise) {
			this.precise = precise;
		}

		public Boolean getAllowDecimal() {
			return allowDecimal;
		}

		public void setAllowDecimal(Boolean allowDecimal) {
			this.allowDecimal = allowDecimal;
		}
	}

	@Override
	public String toString() {
	    return new ToStringBuilder(this).append("externalId", externalId).build();
	}
}
