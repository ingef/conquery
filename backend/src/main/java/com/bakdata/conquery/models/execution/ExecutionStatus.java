package com.bakdata.conquery.models.execution;

import java.time.ZonedDateTime;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@AllArgsConstructor
@Builder
public class ExecutionStatus {

	private String[] tags;
	private String label;
	private ZonedDateTime createdAt;
	private ZonedDateTime lastUsed;
	private UserId owner;
	private String ownerName;
	private boolean shared;
	private boolean own;
	private boolean system;
	private QueryDescription query;

	private ManagedExecutionId id;
	private ExecutionState status;
	private String message;
	private Long numberOfResults;
	private Long requiredTime;
	private String resultUrl;
}
