package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;

import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.Workers;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString @RequiredArgsConstructor
public class UpdateMatchingStats extends Job {

	@ToString.Exclude
	private final Workers workers;

	@Override
	public void execute() throws Exception {
		for(Worker w:new ArrayList<>(workers.getWorkers().values())) {
			
		}
	}

	@Override
	public String getLabel() {
		return toString();
	}

}
