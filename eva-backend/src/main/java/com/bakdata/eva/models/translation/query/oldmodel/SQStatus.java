package com.bakdata.eva.models.translation.query.oldmodel;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @NoArgsConstructor @ToString @AllArgsConstructor @Builder
public class SQStatus {

	private String[] tags = new String[0];
	private String label;
	private ZonedDateTime createdAt;
	private ZonedDateTime lastUsed;
	@JsonIgnore
	private int owner;
	private String ownerName;
	private boolean shared;
	private boolean own;
	private boolean system;
	/** this can hold different payloads. After an sql query 
	  * it will hold the query as a string but this will be mapped to 
	  * a parsed version before giving to to the frontend */
	private Object query;
	
	public enum StatusCode {NEW,RUNNING,CANCELED,FAILED,DONE}

	private UUID id;
	private StatusCode status;
	private String message;
	private Long numberOfResults;
	private Long requiredTime;
	private String resultUrl;
	
	public SQStatus(UUID id, StatusCode status) {
		this(id, status, null, null, null);
	}

	public SQStatus(UUID id, StatusCode status, String message) {
		this(id, status, message, null, null);
	}


	public SQStatus(UUID id, StatusCode status, String message, Long numberOfResults, Long requiredTime) {
		this.id=id;
		this.status = status;
		this.message = message;
		this.numberOfResults = numberOfResults;
		this.requiredTime = requiredTime;
	}

	public SQStatus(UUID id, StatusCode status, long numberOfResults, long requiredTime) {
		this(id, status, null, numberOfResults, requiredTime);
	}

	public SQStatus(UUID id, StatusCode status, String message, Long numberOfResults, Long requiredTime, String resultUrl) {
		this.id = id;
		this.status = status;
		this.message = message;
		this.numberOfResults = numberOfResults;
		this.requiredTime = requiredTime;
		this.resultUrl = resultUrl;
	}
}
