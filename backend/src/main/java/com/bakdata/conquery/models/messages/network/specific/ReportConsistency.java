package com.bakdata.conquery.models.messages.network.specific;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ReportConsistency extends NamespaceMessage {

    private final WorkerId workerId;
    private final Set<ImportId> workerImports;
    private final Set<BucketId> workerBuckets;


    @Override
    public void react(Namespace context) throws Exception {
        Namespace namespace = context.getNamespaces().get(workerId.getDataset());
        Set<ImportId> managerImports = namespace.getStorage().getAllImports().stream().map(Import::getId).collect(Collectors.toSet());

        boolean importsOkay = isImportsConsistent(managerImports, workerImports, workerId);
    }

    private static boolean isImportsConsistent(Set<ImportId> managerImports, Set<ImportId> workerImports, WorkerId workerId) {
        Sets.SetView<ImportId> differences = Sets.difference(managerImports, workerImports);

        if (differences.isEmpty()) {
            log.info("Imports of worker {} are consistent with the imports of manager", workerId);
            return true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found inconsistencies:\n");
        for( ImportId difference : differences) {
            if(!managerImports.contains(difference)) {
                sb.append("\tImport [").append(difference).append("] is not present on the manager but on the worker [").append(workerId).append("].\n");
            }
            else {
                sb.append("\tImport [").append(difference).append("] is not present on the worker but on the manager [").append(workerId).append("].\n");
            }
        }
        log.error(sb.toString());
        return false;
    }
}
