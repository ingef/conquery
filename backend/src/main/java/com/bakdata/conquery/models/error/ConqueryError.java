package com.bakdata.conquery.models.error;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.util.VariableDefaultValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.Flat3Map;
import org.apache.commons.text.StringSubstitutor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for errors that are thrown within Conquery and can be serialized
 * and deserialized to allow transportation between nodes.
 */
@SuppressWarnings("serial")
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "code")
@CPSBase
@ToString(onlyExplicitlyIncluded = true)
public abstract class ConqueryError extends RuntimeException implements ConqueryErrorInfo {
	
	private static final String NO_MEASSAGE = "Unable to provide error message. No message template was provided by error.";
	
	@VariableDefaultValue
	@NotNull
	@ToString.Include
	private UUID id = UUID.randomUUID();
	@NotEmpty
	private String messageTemplate;
	private Map<String, String> context;
	
	
	public ConqueryError(String messageTemplate, Map<String, String> context) {
		this.messageTemplate = messageTemplate;
		this.context = context;
	}
	
	@Override
	@JsonIgnore
	@ToString.Include
	public String getMessage() {
		if(messageTemplate == null) {
			return NO_MEASSAGE;
		}
		return new StringSubstitutor(context).replace(messageTemplate);
	}

	@Override
	public PlainError asPlain() {
		return new PlainError(getId(), getCode(), getMessage(), getContext());
	}

	@Override
	@JsonIgnore // The code is the type information, so we do not need to serialize it
	public final String getCode() {
		return this.getClass().getAnnotation(CPSType.class).id();
	}
	
	/**
	 * Wraps the {@link Throwable} into an {@link ConqueryError}.
	 */
	public static ConqueryError asConqueryError(Throwable t) {
		return t instanceof ConqueryError ? (ConqueryError) t : new ConqueryError.UnknownError(t);
	}

	public static abstract class NoContextError extends ConqueryError {

		public NoContextError(String message) {
			super(message, Collections.emptyMap());
		}
	}

	public static abstract class ContextError extends ConqueryError {

		public ContextError(String messageTemplate) {
			super(messageTemplate, new Flat3Map<>());
		}
	}

	@Slf4j
	@CPSType(base = ConqueryError.class, id = "CQ_UNKNOWN_ERROR")
	public static class UnknownError extends NoContextError {

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private UnknownError() {
			super("An unknown error occured");
		}

		public UnknownError(Throwable e) {
			this();
			log.error("Encountered unknown Error[{}]", this.getId(), e);
		}
	}


	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION")
	public static class ExecutionCreationErrorUnspecified extends NoContextError {

		public ExecutionCreationErrorUnspecified() {
			super("Failure during execution creation.");
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE")
	public static class ExecutionCreationResolveError extends ContextError {

		private final static String FAILED_ELEMENT = "ELEMENT";
		private final static String FAILED_ELEMENT_CLASS = "ELEMENT_CLASS";
		private final static String TEMPLATE = "Could not find an ${" + FAILED_ELEMENT_CLASS + "} element called '${" + FAILED_ELEMENT + "}'";

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private ExecutionCreationResolveError() {
			super(TEMPLATE);
		}

		public ExecutionCreationResolveError(IId<?> unresolvableElementId) {
			this();
			getContext().put(FAILED_ELEMENT, unresolvableElementId.toString());
			getContext().put(FAILED_ELEMENT_CLASS, unresolvableElementId.getClass().getSimpleName());
		}
	}


	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL")
	public static class ExternalResolveError extends ContextError {

		private final static String FORMAT_ROW_LENGTH = "formatRowLength";
		private final static String DATA_ROW_LENGTH = "dataRowLength";
		private final static String TEMPLATE = "There are ${" + FORMAT_ROW_LENGTH + "} columns in the format but ${" + DATA_ROW_LENGTH + "} in at least one row";

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private ExternalResolveError() {
			super(TEMPLATE);
		}

		public ExternalResolveError(int formatRowLength, int dataRowLength) {
			this();
			getContext().put(FORMAT_ROW_LENGTH, Integer.toString(formatRowLength));
			getContext().put(DATA_ROW_LENGTH, Integer.toString(dataRowLength));
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL_FORMAT")
	public static class ExternalResolveFormatError extends ContextError {

		private final static String FORMAT_ROW_LENGTH = "formatRowLength";
		private final static String DATA_ROW_LENGTH = "dataRowLength";
		private final static String TEMPLATE = "There are ${" + FORMAT_ROW_LENGTH + "} columns in the format but ${" + DATA_ROW_LENGTH + "} in at least one row";

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private ExternalResolveFormatError() {
			super(TEMPLATE);
		}

		public ExternalResolveFormatError(int formatRowLength, int dataRowLength) {
			this();
			getContext().put(FORMAT_ROW_LENGTH, Integer.toString(formatRowLength));
			getContext().put(DATA_ROW_LENGTH, Integer.toString(dataRowLength));
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL_EMPTY")
	public static class ExternalResolveEmptyError extends ContextError {

		private final static String TEMPLATE = "None of the provided values could be resolved.";

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		public ExternalResolveEmptyError() {
			super(TEMPLATE);
		}
	}

	/**
	 * Unspecified error during {@link QueryPlan}-creation.
	 */
	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_PLAN")
	public static class ExecutionCreationPlanError extends NoContextError {

		public ExecutionCreationPlanError() {
			super("Unable to generate query plan.");
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_CREATION_PLAN_DATECONTEXT_MISMATCH")
	public static class ExecutionCreationPlanDateContextError extends ContextError {

		private final static String ALIGNMENT = "alignment";
		private final static String RESOLUTION = "resolution";
		private final static String ALIGNMENT_SUPPORTED = "alignmentsSupported";
		private final static String TEMPLATE = "Alignment ${" + ALIGNMENT + "} and resolution ${" + RESOLUTION + "} don't fit together. The resolution only supports these alignments: ${" + ALIGNMENT_SUPPORTED + "}";

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private ExecutionCreationPlanDateContextError() {
			super(TEMPLATE);
		}

		public ExecutionCreationPlanDateContextError(DateContext.Alignment alignment, DateContext.Resolution resolution) {
			this();
			getContext().put(ALIGNMENT, Objects.toString(alignment));
			getContext().put(RESOLUTION, Objects.toString(resolution));
			getContext().put(ALIGNMENT_SUPPORTED, Objects.toString(resolution.getSupportedAlignments()));
		}
	}
	
	/**
	 * Unspecified execution processing error.
	 */
	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_PROCESSING")
	public static class ExecutionProcessingError extends NoContextError {

		public ExecutionProcessingError() {
			super("An unexpected error occured during the execution.");
		}
	}
	
	/**
	 * Timeout during processing.
	 */
	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_PROCESSING_TIMEOUT")
	public static class ExecutionProcessingTimeoutError extends NoContextError {

		public ExecutionProcessingTimeoutError() {
			super("The execution took too long to finish.");
		}
	}
}
