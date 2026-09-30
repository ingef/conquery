package com.bakdata.conquery.models.preproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.StringParser;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.hash.Hashing;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Preprocessed {


	private final PreprocessingJob job;
	private final String name;
	/**
	 * @implSpec this is ALWAYS {@link StringStore}.
	 */
	private final StringParser primaryColumn;

	private final PPColumn[] columns;
	private final TableImportDescriptor descriptor;

	private final ColumnValues[] values;
	/**
	 * Per row store, entity.
	 */
	private final ArrayList<String> rowEntities = new ArrayList<>();

	private long rows;
	private boolean finalized;

	private static final class BucketData {
		private final int bucketId;
		private final Object2IntMap<String> entityStarts;
		private final Object2IntMap<String> entityEnds;
		private int[] sourceEvents;
		private final Map<String, ColumnStore> stores;

		private BucketData(int bucketId, Object2IntMap<String> entityStarts, Object2IntMap<String> entityEnds, int[] sourceEvents, Map<String, ColumnStore> stores) {
			this.bucketId = bucketId;
			this.entityStarts = entityStarts;
			this.entityEnds = entityEnds;
			this.sourceEvents = sourceEvents;
			this.stores = stores;
		}

		private PreprocessedData toPreprocessedData() {
			return new PreprocessedData(bucketId, entityStarts, entityEnds, stores);
		}

		private void releaseSourceEvents() {
			sourceEvents = null;
		}
	}

	public Preprocessed(ConqueryConfig config, PreprocessingJob preprocessingJob) throws IOException {
		job = preprocessingJob;
		descriptor = preprocessingJob.getDescriptor();
		name = descriptor.getName();


		final TableInputDescriptor input = descriptor.getInputs()[0];
		columns = new PPColumn[input.getWidth()];

		primaryColumn = (StringParser) MajorTypeId.STRING.createParser(config);

		values = new ColumnValues[columns.length];

		for (int index = 0; index < input.getWidth(); index++) {
			final ColumnDescription columnDescription = input.getColumnDescription(index);
			columns[index] = new PPColumn(columnDescription.getName(), columnDescription.getType());

			final Parser parser = input.getOutput()[index].createParser(config);
			columns[index].setParser(parser);

			values[index] = parser.createColumnValues();
		}
	}


	public synchronized void write(File file, int buckets) throws IOException {
		ensureNotFinalized();
		finalized = true;

		final Object2IntMap<String> entityLength = new Object2IntAVLTreeMap<>();

		calculateEntityLengths(entityLength);

		final IntSummaryStatistics statistics = entityLength.values().intStream().summaryStatistics();
		log.info("Statistics = {}", statistics);


		log.debug("Writing Headers");

		final Map<Integer, List<String>> bucket2Entity = new TreeMap<>();
		for (String entity : entityLength.keySet()) {
			bucket2Entity.computeIfAbsent(getEntityBucket(buckets, entity), ignored -> new ArrayList<>()).add(entity);
		}


		final int hash = descriptor.calculateValidityHash(job.getCsvDirectory(), job.getTag());

		final PreprocessedHeader header =
				new PreprocessedHeader(descriptor.getName(), descriptor.getTable(), rows, entityLength.size(), bucket2Entity.size(), columns, hash);

		final Deque<BucketData> bucketData = createBucketData(bucket2Entity, entityLength);

		entityLength.clear();
		bucket2Entity.clear();

		populateBucketStores(bucketData);
		writePreprocessed(file, header, bucketData);
	}

	public static int getEntityBucket(int buckets, String id) {
		return Hashing.consistentHash(id.hashCode(), buckets);
	}

	/**
	 * Calculate the number of events per entity.
	 */
	private void calculateEntityLengths(Object2IntMap<String> entityLength) {

		for (String entity : rowEntities) {
			final int curr = entityLength.getOrDefault(entity, 0);
			entityLength.put(entity, curr + 1);
		}
	}

	private Deque<BucketData> createBucketData(Map<Integer, List<String>> bucket2Entities, Object2IntMap<String> entityLengths) {
		final Map<String, IntList> entityEvents = new HashMap<>(entityLengths.size());

		for (int pos = 0, size = rowEntities.size(); pos < size; pos++) {
			final String entity = rowEntities.get(pos);
			entityEvents.computeIfAbsent(entity, (ignored) -> new IntArrayList()).add(pos);
		}
		rowEntities.clear();
		rowEntities.trimToSize();

		final Deque<BucketData> buckets = new ArrayDeque<>(bucket2Entities.size());

		for (Map.Entry<Integer, List<String>> bucket : bucket2Entities.entrySet()) {
			final Object2IntMap<String> entityStarts = new Object2IntOpenHashMap<>();
			final Object2IntMap<String> entityEnds = new Object2IntOpenHashMap<>();
			final IntList sourceEvents = new IntArrayList();

			int currentStart = 0;
			for (String entity : bucket.getValue()) {
				final int length = entityLengths.getInt(entity);
				final IntList events = entityEvents.remove(entity);

				if (events == null || events.size() != length) {
					throw new IllegalStateException("Entity events are not aligned for " + entity);
				}

				entityStarts.put(entity, currentStart);
				entityEnds.put(entity, currentStart + length);
				currentStart += length;

				for (int event : events) {
					sourceEvents.add(event);
				}
			}

			buckets.addLast(new BucketData(bucket.getKey(), entityStarts, entityEnds, sourceEvents.toIntArray(), new HashMap<>()));
		}

		if (!entityEvents.isEmpty()) {
			throw new IllegalStateException("Not all entity events were assigned to a bucket");
		}

		return buckets;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void populateBucketStores(Iterable<BucketData> buckets) {
		for (int columnIndex = 0; columnIndex < columns.length; columnIndex++) {
			final PPColumn column = columns[columnIndex];
			final ColumnValues columnValues = values[columnIndex];

			log.info("Compute best Subtype for Column[{}] with {}", column.getName(), column.getParser());

			try {
				for (BucketData bucket : buckets) {
					final ColumnStore store = column.createStore(bucket.sourceEvents.length);

					if (columnValues != null) {
						for (int localEvent = 0; localEvent < bucket.sourceEvents.length; localEvent++) {
							final int sourceEvent = bucket.sourceEvents[localEvent];

							if (columnValues.isNull(sourceEvent)) {
								store.setNull(localEvent);
							}
							else {
								column.getParser().setValue(store, localEvent, columnValues.get(sourceEvent));
							}
						}
					}

					bucket.stores.put(column.getName(), store);
				}
			}
			finally {
				values[columnIndex] = null;
			}
		}

		for (BucketData bucket : buckets) {
			bucket.releaseSourceEvents();
		}
	}

	private static void writePreprocessed(File file, PreprocessedHeader header, Deque<BucketData> buckets) throws IOException {
		final OutputStream out = new GZIPOutputStream(new FileOutputStream(file));
		try (JsonGenerator generator = Jackson.BINARY_MAPPER.copy().enable(JsonGenerator.Feature.AUTO_CLOSE_TARGET).getFactory().createGenerator(out)) {

			log.debug("Writing header");

			generator.writeObject(header);

			log.debug("Writing data");

			BucketData bucket;
			while ((bucket = buckets.pollFirst()) != null) {
				generator.writeObject(bucket.toPreprocessedData());
			}
		}
	}

	public synchronized String addPrimary(String primary) {
		ensureNotFinalized();
		return primaryColumn.addLine(primary);
	}

	public synchronized void addRow(String primaryId, PPColumn[] columns, Object[] outRow) {
		ensureNotFinalized();
		final int event = rowEntities.size();
		rowEntities.add(primaryId);

		for (int col = 0; col < outRow.length; col++) {

			if (values[col] == null && outRow[col] != null) {
				throw new IllegalStateException(String.format("Expecting %s to be NULL, because no ColumnValues could be generated by the associated parser", outRow[col]));
			}

			if (values[col] == null) {
				continue;
			}
			final int idx = values[col].add(outRow[col]);

			if (event != idx) {
				throw new IllegalStateException("Columns are not aligned");
			}

			if (log.isTraceEnabled()) {
				log.trace("Registering `{}` for Column[{}]", outRow[col], columns[col].getName());
			}
			columns[col].getParser().addLine(outRow[col]);
		}

		//update stats
		rows++;
	}

	private void ensureNotFinalized() {
		if (finalized) {
			throw new IllegalStateException("Preprocessed data has already been finalized");
		}
	}
}
