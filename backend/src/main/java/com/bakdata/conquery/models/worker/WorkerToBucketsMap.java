package com.bakdata.conquery.models.worker;

import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor(onConstructor = @__({@JsonCreator}))
@NoArgsConstructor
@Getter
public class WorkerToBucketsMap {
    private Map<WorkerId, Set<BucketId>> map = new ConcurrentHashMap<>();

    public Set<BucketId> getBucketsForWorker(WorkerId workerId) {
        // Don't modify the underlying map here
        Set<BucketId> buckets = map.get(workerId);
        if (buckets != null) {
            return buckets;
        }
        return Collections.emptySet();
    }

    public void addBucketForWorker(@NonNull WorkerId id, @NonNull Set<BucketId> bucketIds) {
        map.computeIfAbsent(id, k -> new HashSet<>()).addAll(bucketIds);
    }

    public void removeBucketsOfImport(@NonNull ImportId importId) {
        map.values().forEach(set -> set.removeIf(bucketId -> bucketId.getImp().equals(importId)));
    }
}
