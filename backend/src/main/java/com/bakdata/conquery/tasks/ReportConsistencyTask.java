package com.bakdata.conquery.tasks;

import com.bakdata.conquery.models.messages.namespaces.specific.RequestConsistency;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import io.dropwizard.servlets.tasks.Task;
import lombok.RequiredArgsConstructor;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class ReportConsistencyTask extends Task {

    private final DatasetRegistry datasetRegistry;

    public ReportConsistencyTask(DatasetRegistry datasetRegistry) {
        super("report-consistency");
        this.datasetRegistry = datasetRegistry;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
        datasetRegistry.getWorkers().values().forEach(w -> w.send(new RequestConsistency()));
    }
}
