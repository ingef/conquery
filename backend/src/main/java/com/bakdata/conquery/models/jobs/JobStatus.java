package com.bakdata.conquery.models.jobs;

import java.util.Comparator;
import java.util.UUID;

import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobStatus implements Comparable<JobStatus> {

	public static final Comparator<JobStatus> BY_PROGRESS = (left, right) -> Double.compare(right.progressReporter.getProgress(), left.progressReporter.getProgress());
	public static final Comparator<JobStatus> BY_LABEL = Comparator.comparing(JobStatus::getLabel);
	public static final Comparator<JobStatus> BY_UUID = Comparator.comparing(JobStatus::getJobId);


	private UUID jobId;
	private ProgressReporter progressReporter;
	private String label;
	private boolean cancelled;

	@Override
	public int compareTo(@NotNull JobStatus o) {
		return BY_PROGRESS
					   .thenComparing(BY_LABEL)
					   .thenComparing(BY_UUID)
					   .compare(this, o);
	}
}
