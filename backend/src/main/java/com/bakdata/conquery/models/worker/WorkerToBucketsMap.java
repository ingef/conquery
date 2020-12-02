package com.bakdata.conquery.models.worker;

import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class WorkerToBucketsMap {
    private Map<WorkerId, Set<BucketId>> map = new HashMap<>();

    public Set<BucketId> getBucketsForWorker(WorkerId workerId) {
        Set<BucketId> buckets = map.get(workerId);
        if (buckets != null) {
            return buckets;
        }
        return Collections.emptySet();
    }

    public synchronized void addBucketForWorker(@NonNull WorkerId id, @NonNull Set<BucketId> bucketIds) {
        map.computeIfAbsent(id, k -> new HashSet<>()).addAll(bucketIds);
    }
}
