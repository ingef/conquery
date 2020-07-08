package com.bakdata.conquery.models.jobs;

import java.util.UUID;

import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data @NoArgsConstructor @AllArgsConstructor
public class JobStatus implements Comparable<JobStatus> {

	private UUID jobId;
	private ProgressReporter progressReporter;
	private String label;
	private boolean cancelled;

	@Override
	public int compareTo(@NotNull JobStatus o) {
		return Double.compare(o.progressReporter.getProgress(), progressReporter.getProgress());
	}
}
