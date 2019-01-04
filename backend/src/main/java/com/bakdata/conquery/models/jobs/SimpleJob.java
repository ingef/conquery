package com.bakdata.conquery.models.jobs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleJob extends Job {
	@Getter
	private final String label;
	private final Runnable runner;

	@Override
	public void execute() throws Exception {
		runner.run();
	}
}
