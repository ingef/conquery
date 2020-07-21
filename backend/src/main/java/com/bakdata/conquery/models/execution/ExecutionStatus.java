package com.bakdata.conquery.models.execution;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.List;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

@NoArgsConstructor
@ToString
@AllArgsConstructor
@Data
@FieldNameConstants
public class ExecutionStatus {

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
		private URL resultUrl;
		
		/**
		 * Holds a description for each column, present in the result.
		 */
		private List<ColumnDescriptor> columnDescriptions;
		
		/**
		 * Indicates if the concepts that are included in the query description can be accesed by the user.
		 */
		private boolean canExpand;
		
		/**
		 * Is set to the query description if the user can expand all included concepts.
		 */
		private QueryDescription query;
		
		/**
		 * Is set when the QueryFailed
		 */
		private ExecutionError.PlainContext error;
		
		public static enum CreationFlag{
			WITH_COLUMN_DESCIPTION,
			WITH_SOURCE;
		}
}
