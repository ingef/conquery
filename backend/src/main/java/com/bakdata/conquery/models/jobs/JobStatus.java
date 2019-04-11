package com.bakdata.conquery.models.jobs;

import com.bakdata.conquery.util.progressreporter.ProgressReporter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class JobStatus {

	private ProgressReporter progressReporter;
	private String label;
}
