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
	
	@JsonProperty("concept_class")
	private String conceptClass;
	
	private String datatype;
	
	private List<Name> names = new ArrayList<OclConcept.Name>();
	
	private List<Description> descriptons = new ArrayList<OclConcept.Description>();
	
	private boolean retired;
	
	private String source;
	
	@JsonProperty("source_url")
	private String sourceUrl;
	
	private String versionUrl;
	
	@JsonProperty("created_on")
	private Date dateCreated;
	
	@JsonProperty("updated_on")
	private Date dateUpdated;
	
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
	
	public List<Description> getDescriptons() {
		return descriptons;
	}
	
	public void setDescriptons(List<Description> descriptons) {
		this.descriptons = descriptons;
	}
	
	public boolean isRetired() {
		return retired;
	}
	
	public void setRetired(boolean retired) {
		this.retired = retired;
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
	
	public String getVersionUrl() {
		return versionUrl;
	}
	
	public void setVersionUrl(String versionUrl) {
		this.versionUrl = versionUrl;
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
	
	public static class Name {
		
		private Locale locale;
		
		@JsonProperty("locale_preferred")
		private boolean localePreferred;
		
		private String name;
		
		@JsonProperty("name_type")
		private String nameType;
		
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
	
	public static class Description {
		
		private Locale locale;
		
		private String description;
		
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
	
}
