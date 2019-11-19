package com.bakdata.conquery.models.jobs;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class JobManagerStatus {
	@NonNull @NotNull
	private LocalDateTime timestamp;
	@NotNull
	private List<JobStatus> jobs = Collections.emptyList();
	
	public int size() {
		return jobs.size();
	}
	
	@JsonIgnore
	public String getAgeString() {
		Duration duration = Duration.between(timestamp, LocalDateTime.now());
		if(duration.toSeconds()>0) {
			return Long.toString(duration.toSeconds())+" s";
		}
		else {
			return Long.toString(duration.toMillis())+" ms";
		}
	}
}
