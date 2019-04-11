package com.bakdata.conquery.resources.admin.ui;

import java.util.Collection;

import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.models.worker.WorkerInformation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UIContext {

	@Getter
	private final Namespaces namespaces;

	public boolean[] getWorkerStatuses() {
		boolean[] result = new boolean[namespaces.getSlaves().values().size()];
		int id = 0;
		for(WorkerInformation wi:namespaces.getWorkers().values()) {
			result[id++] = wi.isConnected();
		}
		return result;
	}
	
	public Collection<WorkerInformation> getWorkers() {
		return namespaces.getWorkers().values();
	}
}
