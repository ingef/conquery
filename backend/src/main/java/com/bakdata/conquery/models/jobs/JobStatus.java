package com.bakdata.conquery.models.jobs;

import java.util.UUID;

import com.bakdata.conquery.util.progressreporter.ProgressReporter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class JobStatus {

	private UUID jobId;
	private ProgressReporter progressReporter;
	private String label;
	private boolean cancelled;
}
