package com.bakdata.conquery.models.execution;

import java.util.Map;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.util.VariableDefaultValue;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.map.Flat3Map;
import org.hibernate.validator.constraints.NotEmpty;

@Getter
@Setter
@Builder
public class QueryError {
	/**
	 * A unique id for this error to retrieve it in the logs.
	 */
	@Builder.Default
	@VariableDefaultValue
	@NotNull
	private UUID id = UUID.randomUUID();
	@NotEmpty
	private String code;
	private String message;
	@Builder.Default
	private Map<String,String> context = new Flat3Map<>();
	
	public static enum CQErrorCodes{
		UNKNOWN_ERROR,
		QUERY_CREATION_RESOLVE,
		QUERY_CREATION_PLAN,
		QUERY_EXECUTION;
	}
}
