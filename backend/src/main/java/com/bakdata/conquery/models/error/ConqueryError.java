package com.bakdata.conquery.models.error;

import java.util.Set;
import java.util.UUID;

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
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

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
	/**
	 * Since Jackson does not seem to be able to deserialize throwable with super.cause set. We have our own member
	 */
	private ConqueryError conqueryCause;

	protected ConqueryError() {
		this(null);
	}

	protected ConqueryError(ConqueryError conqueryCause) {
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
		return new SimpleErrorInfo(getId(), getCode(), getMessage());
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
		return getMessageTemplate(C10N.get(ErrorMessages.class));
	}

	@JsonIgnore
	public abstract String getMessageTemplate(ErrorMessages errorMessages);

	@Slf4j
	@CPSType(base = ConqueryError.class, id = "CQ_UNKNOWN_ERROR")
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class UnknownError extends ConqueryError {

		public UnknownError(Throwable e) {
			super();
			log.error("Encountered unknown Error[{}]", getId(), e);
		}


		@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.unknownError();
		}
	}


	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION")
	@Data
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class ExecutionCreationErrorUnspecified extends ConqueryError {
		@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.executionCreationUnspecified();
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE")
	@Data
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class ExecutionCreationResolveError extends ConqueryError {

		private final String unknownId;
		private final String clazz;

		public ExecutionCreationResolveError(Id<?> unresolvableElementId) {
			unknownId = unresolvableElementId.toString();
			clazz = unresolvableElementId.getClass().getSimpleName();
		}

		@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.executionCreationResolve(unknownId, clazz);
		}
	}


	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL_FORMAT")
	@Data
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class ExternalResolveFormatError extends ConqueryError {


		private final int formatRowLength;
		private final int dataRowLength;


		@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.externalResolveFormatError(formatRowLength, dataRowLength);
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL_ONE_PER_ROW")
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class ExternalResolveOnePerRowError extends ConqueryError {

		@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.externalEntityUnique();
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL_EMPTY")
	@NoArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class ExternalResolveEmptyError extends ConqueryError {

		@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.externalResolveEmpty();
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_CREATION_PLAN_FLAGS_MISSING")
	@Data
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class ExecutionCreationPlanMissingFlagsError extends ConqueryError {
		private final Set<String> labels;


		@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.missingFlags(String.join(", ", labels));
		}

	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_CREATION_CREATION_PLAN_DATECONTEXT_MISMATCH")
	@Data
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class ExecutionCreationPlanDateContextError extends ConqueryError {

		private final Alignment alignment;
		private final Resolution resolution;


		@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.dateContextMismatch(alignment, resolution);
		}
	}

	/**
	 * Unspecified execution processing error.
	 */
	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_JOB")
	@Data
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class ExecutionJobErrorWrapper extends ConqueryError {

		private final Entity entity;

		public ExecutionJobErrorWrapper(Entity entity, ConqueryError e) {
			super(e);
			this.entity = entity;
		}

		@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.unknownQueryExecutionError(entity);
		}
	}

	/**
	 * Unspecified execution processing error.
	 */
	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_PROCESSING")
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class ExecutionProcessingError extends ConqueryError {

		@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.executionProcessingError();
		}
	}

	/**
	 * Timeout during processing.
	 */
	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_PROCESSING_TIMEOUT")
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class ExecutionProcessingTimeoutError extends ConqueryError {

		@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.executionTimeout();
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_EXECUTION_NO_SECONDARY_ID")
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class NoSecondaryIdSelectedError extends ConqueryError {
				@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.noSecondaryIdSelected();
		}
	}

	@CPSType(base = ConqueryError.class, id = "CQ_SQL_ERROR")
	@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
	public static class SqlError extends ConqueryError {
		private final Exception error;

		@Override
		public String getMessageTemplate(ErrorMessages errorMessages) {
			return errorMessages.sqlError(error);
		}


	}
}
