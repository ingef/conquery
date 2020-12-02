package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.worker.Worker;

import java.util.Set;
import java.util.stream.Collectors;

public class RequestConsistency extends WorkerMessage {

    @Override
    public void react(Worker context) throws Exception {
        // Gather ImportIds
        Set<ImportId> workerImports = context.getStorage().getAllImports().stream().map(Import::getId).collect(Collectors.toSet());

        //TODO Gather BucketIds
        Set<BucketId> workerBuckets = Set.of();

        // Send report
        context.send(new ReportConsistency(context.getInfo().getId(), workerImports, workerBuckets));
    }
}
