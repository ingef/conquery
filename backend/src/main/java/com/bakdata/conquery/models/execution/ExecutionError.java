package com.bakdata.conquery.models.execution;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.util.VariableDefaultValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.Flat3Map;
import org.apache.commons.text.StringSubstitutor;
import org.hibernate.validator.constraints.NotEmpty;

public interface ExecutionError {

	/**
	 * A unique id for this error to retrieve it in the logs.
	 */
	UUID getId();
	
	String getCode();
	
	String getMessage();
	
	Map<String,String> getContext();
	
	PlainContext asPlain();
	
	/**
	 * Base class, that is intended for external serialization, without type information.
	 */
	@RequiredArgsConstructor
	@AllArgsConstructor
	@Getter
	public static class PlainContext implements ExecutionError {
		private final UUID id;
		private final String code;
		private String message;
		private Map<String,String> context;
		
		@Override
		public PlainContext asPlain() {
			return this;
		}
	}
	
	/**
	 * Base class for errors that are thrown within Conquery and can be serialized and deserialized to allow transportation between nodes.
	 */
	@Getter
	@Setter
	@RequiredArgsConstructor
	@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
	@CPSBase
	@ToString(onlyExplicitlyIncluded = true)
	public static abstract class ConqueryExecutionError implements ExecutionError {
		@VariableDefaultValue
		@NotNull
		@ToString.Include
		private UUID id = UUID.randomUUID();
		@NotEmpty
		private final String code;
		private final String messageTemplate;
		private final Map<String,String> context;
		
		@Override
		@JsonIgnore
		@ToString.Include
		public String getMessage() {
			return new StringSubstitutor(context).replace(messageTemplate);
		}
		
		@Override
		public PlainContext asPlain() {
			return new PlainContext(getId(), getCode(), getMessage(), getContext());
		}
		
		public ExecutionException asException() {
			return new ExecutionException(this);
		}
	}
	
	public static abstract class NoContextError extends ConqueryExecutionError {
		
		public NoContextError(String code, String message) {
			super(code, message, Collections.emptyMap());
		}
	}
	
	public static abstract class ContextError extends ConqueryExecutionError {
		
		public ContextError(String code, String messageTemplate) {
			super(code, messageTemplate, new Flat3Map<>());
		}
	}

	@Slf4j
	@CPSType(base = ConqueryExecutionError.class, id = "UNKNOWN_ERROR")
	public static class UnknownError extends NoContextError {

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private UnknownError() {
			super(UnknownError.class.getAnnotation(CPSType.class).id(), "An unknown error occured");
		}
		
		public UnknownError(Throwable e) {
			this();
			log.error("Encountered unknown error [{}]", this.getId(), e);
		}
	}

	@CPSType(base = ConqueryExecutionError.class, id = "QUERY_CREATION_RESOLVE")
	public static class QueryCreationResolveError extends ContextError {
		private final static String FAILED_ELEMENT = "ELEMENT";
		private final static String FAILED_ELEMENT_CLASS = "ELEMENT_CLASS";
		private final static String TEMPLATE = "Could not find an ${" + FAILED_ELEMENT_CLASS + "} element called '${" + FAILED_ELEMENT + "}'";
		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private QueryCreationResolveError() {
			super(QueryCreationResolveError.class.getAnnotation(CPSType.class).id(), TEMPLATE);
		}

		public QueryCreationResolveError(IId<?> unresolvableElementId) {
			this();
			getContext().put(FAILED_ELEMENT, unresolvableElementId.toString());
			getContext().put(FAILED_ELEMENT_CLASS, unresolvableElementId.getClass().getSimpleName());
		}
	}
	
	@CPSType(base = ConqueryExecutionError.class, id = "QUERY_CREATION_PLAN")
	public static class QueryCreationPlanError extends NoContextError {

		public QueryCreationPlanError() {
			super(QueryCreationPlanError.class.getAnnotation(CPSType.class).id(), "Unable to resolve query elements.");
		}
	}
	
	@CPSType(base = ConqueryExecutionError.class, id = "QUERY_EXECUTION")
	public static class QueryExecutionError extends NoContextError {
		
		public QueryExecutionError() {
			super(QueryExecutionError.class.getAnnotation(CPSType.class).id(), "Failure during execution of query plan.");
		}
	}
	
	@CPSType(base = ConqueryExecutionError.class, id = "QUERY_CREATION_RESOLVE_EXTERNAL")
	public static class ExternalResolveError extends ContextError {
		private final static String FORMAT_ROW_LENGTH = "formatRowLength";
		private final static String DATA_ROW_LENGTH = "dataRowLength";
		private final static String TEMPLATE = "There are ${" + FORMAT_ROW_LENGTH + "} columns in the format but ${" + DATA_ROW_LENGTH + "} in at least one row";

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private ExternalResolveError() {
			super(ExternalResolveError.class.getAnnotation(CPSType.class).id(), TEMPLATE);
		}

		public ExternalResolveError(int formatRowLength, int dataRowLength) {
			this();
			getContext().put(FORMAT_ROW_LENGTH, Integer.toString(formatRowLength));
			getContext().put(DATA_ROW_LENGTH, Integer.toString(dataRowLength));
		}
	}
}
