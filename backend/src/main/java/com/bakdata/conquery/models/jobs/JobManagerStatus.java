package com.bakdata.conquery.models.jobs;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;


@Data @RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class JobManagerStatus {
	@NonNull @NotNull
	private final LocalDateTime timestamp = LocalDateTime.now();
	@NotNull
	private final SortedSet<JobStatus> jobs = new TreeSet<>();

	public JobManagerStatus(Collection<? extends JobStatus> jobs){
		this.jobs.addAll(jobs);
	}

	public int size() {
		return jobs.size();
	}

}
