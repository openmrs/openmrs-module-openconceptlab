package org.openmrs.module.openconceptlab.client;

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
	
	@JsonProperty("to_source_name")
	private String toSourceName;
	
	@JsonProperty("to_source_code")
	private String toSourceCode;
	
	public static abstract class MapType {
		
		public static final String Q_AND_A = "Q-AND-A";
		
		public static final String SET = "SET";
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
		return toSourceName;
	}
	
	public void setToSourceName(String toSourceName) {
		this.toSourceName = toSourceName;
	}
	
	public String getToSourceCode() {
		return toSourceCode;
	}
	
	public void setToSourceCode(String toSourceCode) {
		this.toSourceCode = toSourceCode;
	}
	
	public String getToConceptUrl() {
		return toConceptUrl;
	}
	
	public void setToConceptUrl(String toConceptUrl) {
		this.toConceptUrl = toConceptUrl;
	}
	
}
