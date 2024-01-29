package com.bakdata.conquery.models.events;

import java.util.BitSet;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class EventFiltersCache {
	private final Cache<Key, BitSet> results = CacheBuilder.newBuilder().softValues().build();

	@SneakyThrows
	public BitSet get(FilterValue<?> filterValue, Bucket bucket) {
		return results.get(new Key(bucket, filterValue), () -> compute(filterValue, bucket));
	}

	private BitSet compute(FilterValue<?> filterValue, Bucket bucket) {
		log.debug("Calculating events for {} {}", bucket, filterValue);

		EventFilterNode<?> filterNode = ((EventFilterNode<?>) filterValue.createNode());

		//TODO init, nextTable, nextBucket

		BitSet bitSet = new BitSet(bucket.getNumberOfEvents());

		for (int event = 0; event < bucket.getNumberOfEvents(); event++) {
			bitSet.set(event, filterNode.checkEvent(bucket, event));
		}

		return bitSet;
	}

	record Key(Bucket bucket, FilterValue<?> filterValue) {
	}


}
