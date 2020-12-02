package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ReportConsistency extends MessageToManagerNode {

    private WorkerId workerId;
    private Set<ImportId> workerImports;
    private Set<BucketId> workerBuckets;

    @Override
    public void react(NetworkMessageContext.ManagerNodeNetworkContext context) throws Exception {
        Namespace namespace = context.getNamespaces().get(workerId.getDataset());
        Set<ImportId> managerImports = namespace.getStorage().getAllImports().stream().map(Import::getId).collect(Collectors.toSet());

        checkImports(managerImports, workerImports);
    }

    private static void checkImports(Set<ImportId> managerImports, Set<ImportId> workerImports, Worker) {
        Sets.SetView<ImportId> difference = Sets.difference(managerImports, workerImports);

        if (difference.isEmpty()) {
            log.info("Imports of worker {} are consistent with the imports of manager", );
            return;
        }
    }
}
