package com.bakdata.conquery.models.error;

import static com.bakdata.conquery.models.error.ConqueryError.ExecutionCreationPlanDateContextError.RESOLUTION;
import static com.bakdata.conquery.models.error.ConqueryError.ExecutionCreationPlanMissingFlagsError.ALIGNMENT;
import static com.bakdata.conquery.models.error.ConqueryError.ExecutionCreationResolveError.FAILED_ELEMENT;
import static com.bakdata.conquery.models.error.ConqueryError.ExecutionCreationResolveError.FAILED_ELEMENT_CLASS;
import static com.bakdata.conquery.models.error.ConqueryError.ExecutionJobErrorWrapper.ENTITY;
import static com.bakdata.conquery.models.error.ConqueryError.ExternalResolveFormatError.DATA_ROW_LENGTH;
import static com.bakdata.conquery.models.error.ConqueryError.ExternalResolveFormatError.FORMAT_ROW_LENGTH;
import static com.bakdata.conquery.models.error.ConqueryError.SqlError.SQL_ERROR;

import c10n.annotations.En;

public interface ErrorMessages {

	@En("An unknown error occurred")
	String getUnknownError();

	String executionCreationUnspecified();

	@En("Could not find an ${" + FAILED_ELEMENT_CLASS + "} element called '${" + FAILED_ELEMENT + "}'")
	String executionCreationResolve();

	@En("There are ${" + FORMAT_ROW_LENGTH + "} columns in the format but ${" + DATA_ROW_LENGTH + "} in at least one row")
	String externalResolveFormatError();

	@En("Entities must be unique.")
	String externalEntityUnique();

	@En("None of the provided Entities could be resolved.")
	String externalResolveEmpty();

	@En("Do not know labels ${" + ALIGNMENT + "}.")
	String missingFlags();

	@En("Alignment ${" + ALIGNMENT + "} and resolution ${" + RESOLUTION + "} are not compatible.")
	String dateContextMismatch();

	@En("Failed to run query job for entity ${" + ENTITY + "}")
	String unknownQueryExecutionError();

	@En("Unexpected error while processing execution.")
	String executionProcessingError();

	@En("Query took too long.")
	String executionTimeout();

	@En("No secondaryId could be selected.")
	String noSecondaryIdSelected();

	@En("Something went wrong while querying the database: ${" + SQL_ERROR + "}.")
	String sqlError();
}
