package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.messages.namespaces.specific.RequestConsistency;
import com.bakdata.conquery.models.worker.DistributedDatasetRegistry;
import io.dropwizard.servlets.tasks.Task;

public class ReportConsistencyTask extends Task {

    private final DistributedDatasetRegistry datasetRegistry;

    public ReportConsistencyTask(DistributedDatasetRegistry datasetRegistry) {
        super("report-consistency");
        this.datasetRegistry = datasetRegistry;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
        datasetRegistry.getDatasets().stream()
            .flatMap(ns -> ns.getWorkerHandler().getWorkers().values().stream())
            .forEach(w -> w.send(new RequestConsistency()));
    }
}
