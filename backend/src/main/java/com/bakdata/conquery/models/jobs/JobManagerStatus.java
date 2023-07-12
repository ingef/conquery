package com.bakdata.conquery.models.jobs;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import org.apache.commons.lang3.time.DurationFormatUtils;


@Data
@AllArgsConstructor(onConstructor_ = @JsonCreator)
public class JobManagerStatus {
	@With
	@Nullable
	private final String origin;
	@Nullable
	private final DatasetId dataset;
	@NotNull
	@EqualsAndHashCode.Exclude
	private final LocalDateTime timestamp;
	@NotNull
	@EqualsAndHashCode.Exclude
	private final List<JobStatus> jobs;

	public JobManagerStatus(String origin, DatasetId dataset, List<JobStatus> statuses) {
		this(origin, dataset, LocalDateTime.now(), statuses);
	}

	public int size() {
		return jobs.size();
	}

	// Used in AdminUIResource/jobs
	@JsonIgnore
	public String getAgeString() {
		final Duration duration = Duration.between(timestamp, LocalDateTime.now());

		return DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true);
	}
}
