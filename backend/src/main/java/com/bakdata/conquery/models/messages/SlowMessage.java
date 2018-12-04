package com.bakdata.conquery.models.messages;

import com.bakdata.conquery.util.progress.reporter.ProgressReporter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface SlowMessage extends Message {

	@JsonIgnore
	ProgressReporter getProgressReporter();
	void setProgressReporter(ProgressReporter progressReporter);
	
	@Override @JsonIgnore
	default boolean isSlowMessage() {
		return true;
	}
}