package com.bakdata.conquery.models.error;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.forms.util.Alignment;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.util.VariableDefaultValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.Flat3Map;
import org.apache.commons.text.StringSubstitutor;

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

	/**
	 * Since Jackson does not seem to be able to deserialize throwable with super.cause set. We have our own member
	 */
	private ConqueryError conqueryCause;


	public ConqueryError(String messageTemplate, Map<String, String> context) {
		this(messageTemplate, context, null);
	}

	public ConqueryError(String messageTemplate, Map<String, String> context, ConqueryError conqueryCause) {
		this.conqueryCause = conqueryCause;
		this.messageTemplate = messageTemplate;
		this.context = context;
	}

	@Override
	@JsonIgnore
	@ToString.Include
	public String getMessage() {
		if (messageTemplate == null) {
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

	public abstract static class NoContextError extends ConqueryError {

		public NoContextError(String message) {
			super(message, Collections.emptyMap());
		}
	}

	public static class ContextError extends ConqueryError {

		public ContextError(String messageTemplate) {
			this(messageTemplate, null);
		}

		public ContextError(String messageTemplate, ConqueryError cause) {
			super(messageTemplate, new Flat3Map<>(), cause);
		}

		public ContextError(String messageTemplate, Map<String, String> context, ConqueryError cause) {
			super(messageTemplate, context, cause);
		}

		public static ConqueryError fromErrorInfo(ConqueryErrorInfo info) {
			if (info instanceof ConqueryError) {
				return (ConqueryError) info;
			}

			return new ConqueryError.ContextError(info.getMessage(), info.getContext(), null);

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

		private static final String FAILED_ELEMENT = "ELEMENT";
		private static final String FAILED_ELEMENT_CLASS = "ELEMENT_CLASS";
		private static final String TEMPLATE = "Could not find an ${" + FAILED_ELEMENT_CLASS + "} element called '${" + FAILED_ELEMENT + "}'";

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private ExecutionCreationResolveError() {
			super(TEMPLATE);
		}

		public ExecutionCreationResolveError(Id<?> unresolvableElementId) {
			this();
			getContext().put(FAILED_ELEMENT, unresolvableElementId.toString());
			getContext().put(FAILED_ELEMENT_CLASS, unresolvableElementId.getClass().getSimpleName());
		}
	}


	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL")
	public static class ExternalResolveError extends ContextError {

		private static final String FORMAT_ROW_LENGTH = "formatRowLength";
		private static final String DATA_ROW_LENGTH = "dataRowLength";
		private static final String
				TEMPLATE =
				"There are ${" + FORMAT_ROW_LENGTH + "} columns in the format but ${" + DATA_ROW_LENGTH + "} in at least one row";

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

		private static final String FORMAT_ROW_LENGTH = "formatRowLength";
		private static final String DATA_ROW_LENGTH = "dataRowLength";
		private static final String
				TEMPLATE =
				"There are ${" + FORMAT_ROW_LENGTH + "} columns in the format but ${" + DATA_ROW_LENGTH + "} in at least one row";

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

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL_ONE_PER_ROW")
	public static class ExternalResolveOnePerRowError extends NoContextError {
		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		public ExternalResolveOnePerRowError() {
			super("External was flagged as one row per entity, but at least one entity spans multiple rows");
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL_EMPTY")
	public static class ExternalResolveEmptyError extends ContextError {

		private static final String TEMPLATE = "None of the provided values could be resolved.";

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

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_CREATION_PLAN_FLAGS_MISSING")
	@FieldNameConstants(level = AccessLevel.PRIVATE)
	public static class ExecutionCreationPlanMissingFlagsError extends ContextError {

		private final Void ALIGNMENT = null;
		private static final String TEMPLATE = "Do not know labels ${" + Fields.ALIGNMENT + "}.";

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private ExecutionCreationPlanMissingFlagsError() {
			super(TEMPLATE);
		}

		public ExecutionCreationPlanMissingFlagsError(Set<String> labels) {
			this();
			getContext().put(Fields.ALIGNMENT, String.join(", ", labels).trim());
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_CREATION_PLAN_DATECONTEXT_MISMATCH")
	public static class ExecutionCreationPlanDateContextError extends ContextError {

		private static final String ALIGNMENT = "alignment";
		private static final String RESOLUTION = "resolution";
		private static final String TEMPLATE = "Alignment ${" + ALIGNMENT + "} and resolution ${" + RESOLUTION + "} are not compatible.";

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private ExecutionCreationPlanDateContextError() {
			super(TEMPLATE);
		}

		public ExecutionCreationPlanDateContextError(Alignment alignment, Resolution resolution) {
			this();
			getContext().put(ALIGNMENT, Objects.toString(alignment));
			getContext().put(RESOLUTION, Objects.toString(resolution));
		}
	}

	/**
	 * Unspecified execution processing error.
	 */
	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_JOB")
	public static class ExecutionJobErrorWrapper extends ContextError {

		private static final String ENTITY = "entity";
		private static final String TEMPLATE = "Failed to run query job for entity ${" + ENTITY + "}";


		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private ExecutionJobErrorWrapper() {
			super(TEMPLATE);
		}


		private ExecutionJobErrorWrapper(ConqueryError e) {
			super(TEMPLATE, e);
		}

		public ExecutionJobErrorWrapper(Entity entity, ConqueryError e) {
			this(e);
			getContext().put(ENTITY, Integer.toString(entity.getId()));

		}
	}

	/**
	 * Execution processing error with individual context.
	 */
	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_PROCESSING_CONTEXT")
	public static class ExecutionProcessingContextError extends ContextError {

		public ExecutionProcessingContextError(String messageTemplate, Map<String, String> context, ConqueryError cause) {
			super(messageTemplate, context, cause);
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

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_NO_SECONDARY_ID")
	public static class NoSecondaryIdSelectedError extends NoContextError {
		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		public NoSecondaryIdSelectedError() {
			super("No SecondaryId was selected");

		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_SQL_ERROR")
	public static class SqlError extends ContextError {

		private static final String SQL_ERROR = "ERROR";

		private static final String TEMPLATE = "Something went wrong while querying the database: ${" + SQL_ERROR + "}.";

		@JsonCreator
		private SqlError() {
			super(TEMPLATE);
		}

		public SqlError(SQLException sqlException) {
			this();
			getContext().put(SQL_ERROR, sqlException.toString());
		}

	}
}
