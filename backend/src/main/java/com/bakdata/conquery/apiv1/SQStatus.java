package com.bakdata.conquery.apiv1;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryStatus;

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
public class SQStatus {

	private String[] tags;
	private String label;
	private ZonedDateTime createdAt;
	private ZonedDateTime lastUsed;
	private UserId owner;
	private String ownerName;
	private boolean shared;
	private boolean own;
	private boolean system;
	/**
	 * this can hold different payloads. After an sql query it will hold the query as a string but this will be mapped to a parsed version before
	 * giving to to the frontend
	 */
	private Object query;

	private ManagedQueryId id;
	private QueryStatus status;
	private String message;
	private Long numberOfResults;
	private Long requiredTime;
	private String resultUrl;

	public SQStatus(ManagedQueryId id, QueryStatus status) {
		this(id, status, null, null, null);
	}

	public SQStatus(ManagedQueryId id, QueryStatus status, String message) {
		this(id, status, message, null, null);
	}

	public SQStatus(ManagedQueryId id, QueryStatus status, String message, Long numberOfResults, Long requiredTime) {
		this.id = id;
		this.status = status;
		this.message = message;
		this.numberOfResults = numberOfResults;
		this.requiredTime = requiredTime;
	}

	public SQStatus(ManagedQueryId id, QueryStatus status, long numberOfResults, long requiredTime) {
		this(id, status, null, numberOfResults, requiredTime);
	}

	public SQStatus(ManagedQueryId id, QueryStatus status, String message, Long numberOfResults, Long requiredTime, String resultUrl) {
		this.id = id;
		this.status = status;
		this.message = message;
		this.numberOfResults = numberOfResults;
		this.requiredTime = requiredTime;
		this.resultUrl = resultUrl;
	}
	
	public static SQStatus buildFromQuery(MasterMetaStorage storage, ManagedQuery query) {
		return buildFromQuery(storage, query, null);
	}
	
	public static SQStatus buildFromQuery(MasterMetaStorage storage, ManagedQuery query, URLBuilder urlb) {
		Long numberOfResults = Long.valueOf(query.fetchContainedEntityResult().count());
		return builder()
			.label(query.getLabel())
			.tags(query.getTags())
			.id(query.getId())
			.own(true)
			.createdAt(query.getCreationTime().atZone(ZoneId.systemDefault()))
			.query(query)
			.requiredTime((query.getStartTime() != null && query.getFinishTime() != null)
				? ChronoUnit.MILLIS.between(query.getStartTime(), query.getFinishTime())
				: null)
			.status(query.getStatus())
			.numberOfResults(numberOfResults > 0 ? numberOfResults : query.getLastResultCount())
			.shared(query.isShared())
			.owner(Optional.ofNullable(query.getOwner()).orElse(null))
			.ownerName(Optional.ofNullable(query.getOwner()).map(user -> storage.getUser(user).getLabel()).orElse(null))
			.resultUrl(
				urlb != null
				? urlb
					.set(ResourceConstants.DATASET, query.getDataset().getName())
					.set(ResourceConstants.QUERY, query.getId().toString())
					.to(ResultCSVResource.GET_CSV_PATH).get()
				: null
			)
			.build();
	}
	
}
