package org.openmrs.module.openconceptlab;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.openmrs.ConceptName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OclConcept {
	
	private String type;
	
	private String uuid;
	
	private String id;
	
	@JsonProperty("concept_class")
	private String conceptClass;
	
	private String datatype;
	
	private List<Name> names;
	
	private List<Description> descriptons;
	
	private boolean retired;
	
	private String source;
	
	@JsonProperty("source_url")
	private String sourceUrl;
	
	@JsonProperty("versionUrl")
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
	}
	
	public static class Description {
		
		private Locale locale;
		
		private boolean localePreferred;
		
		private String description;
		
		@JsonProperty("description_type")
		private String descriptionType;
		
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
		
		public String getDescription() {
			return description;
		}
		
		public void setDescription(String description) {
			this.description = description;
		}
		
		public String getDescriptionType() {
			return descriptionType;
		}
		
		public void setDescriptionType(String descriptionType) {
			this.descriptionType = descriptionType;
		}
	}
	
}
