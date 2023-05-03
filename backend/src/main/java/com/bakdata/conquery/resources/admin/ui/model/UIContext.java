package com.bakdata.conquery.resources.admin.ui.model;

import java.util.Collection;

import com.bakdata.conquery.models.worker.DistributedDatasetRegistry;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.resources.ResourceConstants;
import freemarker.template.TemplateModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UIContext {
	
	private static final TemplateModel STATIC_URI_ELEMENTS = ResourceConstants.getAsTemplateModel();

	@Getter
	private final DistributedDatasetRegistry namespaces;

	@Getter
	public final TemplateModel staticUriElem = STATIC_URI_ELEMENTS;

	public boolean[] getWorkerStatuses() {
		boolean[] result = new boolean[namespaces.getShardNodes().values().size()];
		int id = 0;
		for (WorkerInformation wi : this.getWorkers()) {
			result[id++] = wi.isConnected();
		}
		return result;
	}
	
	public Collection<WorkerInformation> getWorkers() {
		return namespaces.getDatasets().stream()
			.flatMap(ns -> ns.getWorkerHandler().getWorkers().values().stream())
			.toList();
	}
}
