package com.bakdata.conquery.models.error;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import javax.validation.constraints.NotNull;

import c10n.C10N;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.forms.util.Alignment;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.util.VariableDefaultValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
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

	private static final String NO_MESSAGE = "Unable to provide error message. No message template was provided by error.";

	@VariableDefaultValue
	@NotNull
	@ToString.Include
	private UUID id = UUID.randomUUID();
	private Map<String, String> context;
	/**
	 * Since Jackson does not seem to be able to deserialize throwable with super.cause set. We have our own member
	 */
	private ConqueryError conqueryCause;

	protected ConqueryError() {
		this(Collections.emptyMap(), null);
	}

	protected ConqueryError(Map<String, String> context, ConqueryError conqueryCause) {
		this.context = context;
		this.conqueryCause = conqueryCause;
	}

	/**
	 * Wraps the {@link Throwable} into an {@link ConqueryError}.
	 */
	public static ConqueryError asConqueryError(Throwable t) {
		return t instanceof ConqueryError ? (ConqueryError) t : new ConqueryError.UnknownError(t);
	}

	@Override
	public SimpleErrorInfo asPlain() {
		return new SimpleErrorInfo(
				getId(),
				getCode(),
				getMessage()
		);
	}

	@Override
	@JsonIgnore // The code is the type information, so we do not need to serialize it
	public final String getCode() {
		return this.getClass().getAnnotation(CPSType.class).id();
	}

	@Override
	@JsonIgnore
	@ToString.Include
	public final String getMessage() {
		return StringSubstitutor.replace(getMessageTemplate().apply(C10N.get(ErrorMessages.class)), context);
	}

	public abstract Function<ErrorMessages, String> getMessageTemplate();

	@Slf4j
	@CPSType(base = ConqueryError.class, id = "CQ_UNKNOWN_ERROR")
	public static class UnknownError extends ConqueryError {

		public UnknownError(Throwable e) {
			this();
			log.error("Encountered unknown Error[{}]", getId(), e);
		}

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private UnknownError() {
			super();
		}

		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::getUnknownError;
		}
	}


	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION")
	public static class ExecutionCreationErrorUnspecified extends ConqueryError {

		public ExecutionCreationErrorUnspecified() {
			super();
		}

		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::executionCreationUnspecified;
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE")
	public static class ExecutionCreationResolveError extends ConqueryError {

		static final String FAILED_ELEMENT = "ELEMENT";
		static final String FAILED_ELEMENT_CLASS = "ELEMENT_CLASS";

		public ExecutionCreationResolveError(Id<?> unresolvableElementId) {
			super(
					Map.of(
							FAILED_ELEMENT, unresolvableElementId.toString(),
							FAILED_ELEMENT_CLASS, unresolvableElementId.getClass().getSimpleName()
					),
					null
			);

		}

		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::executionCreationResolve;
		}
	}


	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL_FORMAT")
	public static class ExternalResolveFormatError extends ConqueryError {

		static final String FORMAT_ROW_LENGTH = "formatRowLength";
		static final String DATA_ROW_LENGTH = "dataRowLength";


		public ExternalResolveFormatError(int formatRowLength, int dataRowLength) {
			super(Map.of(
					FORMAT_ROW_LENGTH, Integer.toString(formatRowLength),
					DATA_ROW_LENGTH, Integer.toString(dataRowLength)
			), null);
		}

		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::externalResolveFormatError;
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL_ONE_PER_ROW")
	public static class ExternalResolveOnePerRowError extends ConqueryError {
		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		public ExternalResolveOnePerRowError() {
			super();
		}

		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::externalEntityUnique;
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL_EMPTY")
	public static class ExternalResolveEmptyError extends ConqueryError {

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		public ExternalResolveEmptyError() {
			super();
		}


		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::externalResolveEmpty;
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_CREATION_PLAN_FLAGS_MISSING")
	public static class ExecutionCreationPlanMissingFlagsError extends ConqueryError {
		static final String ALIGNMENT = "ALIGNMENT";

		public ExecutionCreationPlanMissingFlagsError(Set<String> labels) {
			super(Map.of(ALIGNMENT, String.join(", ", labels).trim()), null);
		}

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private ExecutionCreationPlanMissingFlagsError() {
			super();
		}

		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::missingFlags;
		}

	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_CREATION_PLAN_DATECONTEXT_MISMATCH")
	public static class ExecutionCreationPlanDateContextError extends ConqueryError {

		static final String RESOLUTION = "resolution";
		private static final String ALIGNMENT = "alignment";


		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private ExecutionCreationPlanDateContextError() {
			super();
		}

		public ExecutionCreationPlanDateContextError(Alignment alignment, Resolution resolution) {
			super(Map.of(
					ALIGNMENT, Objects.toString(alignment),
					RESOLUTION, Objects.toString(resolution)
			), null);
		}

		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::dateContextMismatch;
		}
	}

	/**
	 * Unspecified execution processing error.
	 */
	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_JOB")
	public static class ExecutionJobErrorWrapper extends ConqueryError {

		static final String ENTITY = "entity";

		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		private ExecutionJobErrorWrapper() {
			super();
		}


		public ExecutionJobErrorWrapper(Entity entity, ConqueryError e) {
			super(Map.of(ENTITY, Integer.toString(entity.getId())), e);
		}

		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::unknownQueryExecutionError;
		}
	}

	/**
	 * Unspecified execution processing error.
	 */
	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_PROCESSING")
	public static class ExecutionProcessingError extends ConqueryError {

		public ExecutionProcessingError() {
			super(null, null);
		}

		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::executionProcessingError;
		}
	}

	/**
	 * Timeout during processing.
	 */
	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_PROCESSING_TIMEOUT")
	public static class ExecutionProcessingTimeoutError extends ConqueryError {

		public ExecutionProcessingTimeoutError() {
			super();
		}

		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::executionTimeout;
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_NO_SECONDARY_ID")
	public static class NoSecondaryIdSelectedError extends ConqueryError {
		/**
		 * Constructor for deserialization.
		 */
		@JsonCreator
		public NoSecondaryIdSelectedError() {
			super();
		}

		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::noSecondaryIdSelected;
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_SQL_ERROR")
	public static class SqlError extends ConqueryError {

		static final String SQL_ERROR = "ERROR";


		public SqlError(SQLException sqlException) {
			super(Map.of(SQL_ERROR, sqlException.toString()), null);
		}

		@JsonCreator
		private SqlError() {
			super();
		}

		@Override
		public Function<ErrorMessages, String> getMessageTemplate() {
			return ErrorMessages::sqlError;
		}


	}
}
