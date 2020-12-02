package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.worker.Namespace;
import com.google.common.collect.Sets;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compares the the ids of imports and buckets of a {@link com.bakdata.conquery.models.worker.Worker} with the those
 * the {@link com.bakdata.conquery.commands.ManagerNode} assumed the Worker to have and reports an error if there are
 * inconsistencies.
 */
@CPSType(id="REPORT_CONSISTENCY", base= NamespacedMessage.class)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Slf4j
public class ReportConsistency extends NamespaceMessage {

    private WorkerId workerId;
    // Set default here because an empty set send by the worker is not set (it is null) after deserialization
    private Set<ImportId> workerImports = Set.of();
    private Set<BucketId> workerBuckets = Set.of();


    @Override
    public void react(Namespace context) throws Exception {
        Set<ImportId> managerImports = context.getStorage().getAllImports().stream().map(Import::getId).collect(Collectors.toSet());

        Set<BucketId> assignedWorkerBuckets = context.getBucketsForWorker(workerId);

        boolean importsOkay = isImportsConsistent("Imports", managerImports, workerImports, workerId);
        boolean bucketsOkay = isImportsConsistent("Buckets", assignedWorkerBuckets, workerBuckets, workerId);

        log.trace("Imports on worker[{}}: {}", workerId, workerImports);
        log.trace("Buckets on worker[{}}: {}", workerId, workerBuckets);

        if (importsOkay && bucketsOkay) {
            log.info("Consistency check was successful");
            return;
        }
        throw new IllegalStateException("Detected inconsistency between manager and worker [" + workerId + "]");
    }

    private static <ID extends IId<?>> boolean isImportsConsistent(String typeName, @NonNull Set<ID> managerIds, @NonNull Set<ID> workerIds, WorkerId workerId) {
        Sets.SetView<ID> notInWorker = Sets.difference(managerIds, workerIds);
        Sets.SetView<ID> notInManager = Sets.difference(workerIds, managerIds);

        if (notInWorker.isEmpty() && notInManager.isEmpty()) {
            log.info("{} of worker {} are consistent with the imports of the manager.", typeName, workerId);
            return true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found inconsistencies for").append(typeName).append(":\n");
        for( ID difference : notInWorker) {
            sb.append("\t[").append(difference).append("] is not present on the worker but on the manager [").append(workerId).append("].\n");
        }
        for( ID difference : notInManager) {
            sb.append("\t[").append(difference).append("] is not present on the manager but on the worker [").append(workerId).append("].\n");
        }

        log.error(sb.toString());
        return false;
    }
}
