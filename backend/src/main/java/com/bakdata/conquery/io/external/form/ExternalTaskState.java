package com.bakdata.conquery.io.external.form;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.models.error.SimpleErrorInfo;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;

@Data
public class ExternalTaskState {

	@JsonAlias("task_id")
	private final UUID id;

	/**
	 * Exists for every response.
	 */
	@NotNull
	private final TaskStatus status;

	/**
	 * The execution progress between 0 and 1.
	 * Only set when {@link ExternalTaskState#status} {@code = SUCCESS|RUNNING}.
	 */
	@DecimalMin("0.0")
	@DecimalMax("1.0")
	private final BigDecimal progress;

	/**
	 * The result url.
	 * Only set when {@link ExternalTaskState#status} {@code = SUCCESS}.
	 */
	@Valid
	private List<@Valid ResultAsset> results;

	/**
	 * Short description of the possible Error.
	 * Only set when {@link ExternalTaskState#status} {@code = FAILURE}.
	 */
	private final SimpleErrorInfo error;

	@JsonIgnore
	@ValidationMethod(message = "Invalid 'taskId' for provided state")
	public boolean isValidTaskId() {
		if (status.equals(TaskStatus.FAILURE)) {
			return true;
		}
		return id != null;
	}

	@JsonIgnore
	@ValidationMethod(message = "Status is set to FAILURE, but no error information was set.")
	public boolean isErrorInfoSet() {
		return (status == TaskStatus.FAILURE) ==  (error != null);
	}


		@JsonIgnore
	@ValidationMethod(message = "Result assets don't have unique ids")
	public boolean isResultAssetIdUnique() {
		return results.stream().map(ResultAsset::getAssetId).distinct().count() == results.size();
	}

	@JsonIgnore
	@ValidationMethod(message = "Result assets don't have unique labels")
	public boolean isResultAssetLabelUnique() {
		return results.stream().map(ResultAsset::label).distinct().count() == results.size();
	}

}
