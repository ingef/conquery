package com.bakdata.conquery.models.messages;

import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface SlowMessage extends Message {

	@JsonIgnore
	ProgressReporter getProgressReporter();
	void setProgressReporter(ProgressReporter progressReporter);
}