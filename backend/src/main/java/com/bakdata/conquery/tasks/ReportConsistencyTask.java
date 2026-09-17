package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.messages.namespaces.specific.RequestConsistency;
import io.dropwizard.servlets.tasks.Task;

public class ReportConsistencyTask extends Task {

    private final ClusterState clusterState;

    public ReportConsistencyTask(ClusterState clusterState) {
        super("report-consistency");
        this.clusterState = clusterState;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
		clusterState.getWorkerHandlers().values().stream()
					.flatMap(ns -> ns.getWorkers().stream())
					.forEach(worker -> worker.send(new RequestConsistency()));
    }
}
