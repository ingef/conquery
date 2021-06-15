package com.bakdata.conquery.apiv1;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

@NoArgsConstructor
@ToString
@Data
@FieldNameConstants
public abstract class ExecutionStatus {

	private String[] tags;
	private String label;
	@JsonProperty("isPristineLabel")
	private boolean isPristineLabel;
	private ZonedDateTime createdAt;
	private ZonedDateTime lastUsed;
	private UserId owner;
	private String ownerName;
	private boolean shared;
	private boolean own;
	private boolean system;

	private ManagedExecutionId id;
	private ExecutionState status;
	private Long numberOfResults;
	private Long requiredTime;

	private String queryType;
	private SecondaryIdDescriptionId secondaryId;


	/**
	 * The urls under from which the result of the execution can be downloaded as soon as it finished successfully.
	 */
	private List<URL> resultUrls = Collections.emptyList();


}
