package com.bakdata.conquery.models.execution;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.error.ConqueryErrorInfo;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
	 * The url under from which the result of the execution can be downloaded as soon as it finished successfully.
	 */
	private URL resultUrl;

	/**
	 * Light weight description of an execution. Rendering the overview should not cause heavy computations.
	 */
	@NoArgsConstructor
	public static class Overview extends ExecutionStatus {

	}


	/**
	 * This status holds extensive information about the query description and meta data that is computational heavy
	 * and can produce a larger payload to requests.
	 * It should only be rendered, when a client asks for a specific execution, not if a list of executions is requested.
	 */
	@NoArgsConstructor
	@Data
	@EqualsAndHashCode(callSuper = true)
	@FieldNameConstants
	public static class Full extends ExecutionStatus {

        /**
         * Holds a description for each column, present in the result.
         */
        private List<ColumnDescriptor> columnDescriptions;

        /**
         * Indicates if the concepts that are included in the query description can be accessed by the user.
         */
        private boolean canExpand;

        /**
         * Is set to the query description if the user can expand all included concepts.
         */
        private QueryDescription query;

        /**
         * Is set when the QueryFailed
         */
        private ConqueryErrorInfo error;

        /**
         * The groups this execution is shared with.
         */
        private Collection<GroupId> groups;

        @NsIdRefCollection
		private Set<SecondaryIdDescription> availableSecondaryIds;
	}
}
