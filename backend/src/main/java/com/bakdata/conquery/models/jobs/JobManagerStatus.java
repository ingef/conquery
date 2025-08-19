package com.bakdata.conquery.models.jobs;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.time.DurationFormatUtils;


@Data
@AllArgsConstructor(onConstructor_ = @JsonCreator)
@Builder(toBuilder = true)
public class JobManagerStatus {
	@Nullable
	private final String origin;
	@Nullable
	private final DatasetId dataset;
	@NotNull
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private final LocalDateTime timestamp = LocalDateTime.now();
	@NotNull
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private final List<JobStatus> jobs = Collections.emptyList();


	public int size() {
		return jobs.size();
	}

	@JsonIgnore
	public Duration getAge() {
		return Duration.between(timestamp, LocalDateTime.now());
	}

	// Used in AdminUIResource/jobs
	@JsonIgnore
	public String getAgeString() {

		return DurationFormatUtils.formatDurationWords(getAge().toMillis(), true, true);
	}
}
