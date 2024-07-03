package com.bakdata.conquery.models.preproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
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
	private final List<String> rowEntities = new ArrayList<>();


	private long rows;

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


	public void write(File file, int buckets) throws IOException {

		final Object2IntMap<String> entityStart = new Object2IntAVLTreeMap<>();
		final Object2IntMap<String> entityLength = new Object2IntAVLTreeMap<>();

		calculateEntitySpans(entityStart, entityLength);

		final IntSummaryStatistics statistics = entityLength.values().intStream().summaryStatistics();
		log.info("Statistics = {}", statistics);


		final Map<String, ColumnStore> columnStores = combineStores(entityStart);


		log.debug("Writing Headers");

		//TODO this could actually be done at read-time, avoiding large allocations entirely. But in a different smaller PR.
		final Map<Integer, Collection<String>> bucket2Entity = entityStart.keySet().stream()
																		  .collect(Collectors.groupingBy(id -> getEntityBucket(buckets, id)))
																		  .entrySet().stream()
																		  .collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashSet<>(entry.getValue())));


		final int hash = descriptor.calculateValidityHash(job.getCsvDirectory(), job.getTag());

		final PreprocessedHeader header =
				new PreprocessedHeader(descriptor.getName(), descriptor.getTable(), rows, entityStart.size(), bucket2Entity.size(), columns, hash);

		writePreprocessed(file, header, entityStart, entityLength, columnStores, bucket2Entity);
	}

	public static int getEntityBucket(int buckets, String id) {
		return Hashing.consistentHash(id.hashCode(), buckets);
	}

	/**
	 * Calculate beginning and length of entities in output data.
	 */
	private void calculateEntitySpans(Object2IntMap<String> entityStart, Object2IntMap<String> entityLength) {

		// Count the number of events for the entity
		for (String entity : rowEntities) {
			final int curr = entityLength.getOrDefault(entity, 0);
			entityLength.put(entity, curr + 1);
		}

		// Lay out the entities in order, adding their length.
		int outIndex = 0;

		for (Object2IntMap.Entry<String> entry : entityLength.object2IntEntrySet()) {
			entityStart.put(entry.getKey(), outIndex);
			outIndex += entry.getIntValue();
		}
	}

	/**
	 * Combine raw by-Entity data into column stores, appropriately formatted.
	 */
	@SuppressWarnings("rawtypes")
	private Map<String, ColumnStore> combineStores(Object2IntMap<String> entityStart) {
		final Map<String, ColumnStore> columnStores = Arrays.stream(columns).parallel().collect(Collectors.toMap(PPColumn::getName, PPColumn::findBestType));

		// This object can be huge!
		final Map<String, IntList> entityEvents = new HashMap<>(entityStart.size());

		for (int pos = 0, size = rowEntities.size(); pos < size; pos++) {
			final String entity = rowEntities.get(pos);
			entityEvents.computeIfAbsent(entity, (ignored) -> new IntArrayList()).add(pos);
		}

		for (int colIdx = 0; colIdx < columns.length; colIdx++) {
			final PPColumn ppColumn = columns[colIdx];
			final ColumnValues columnValues = values[colIdx];

			//No need to preprocess the column further more, if it does not contain values, likely backed by a compound ColumnStore
			if (columnValues == null) {
				continue;
			}
			final ColumnStore store = columnStores.get(ppColumn.getName());

			entityStart.object2IntEntrySet().forEach(entry -> {
				final String entity = entry.getKey();
				int outIndex = entry.getIntValue();

				final IntList events = entityEvents.getOrDefault(entity, IntLists.emptyList());

				for (int inIndex : events) {
					if (columnValues.isNull(inIndex)) {
						store.setNull(outIndex);
					}
					else {
						final Object raw = columnValues.get(inIndex);
						ppColumn.getParser().setValue(store, outIndex, raw);
					}
					outIndex++;
				}
			});
		}
		return columnStores;
	}

	private static void writePreprocessed(File file, PreprocessedHeader header, Map<String, Integer> globalStarts, Map<String, Integer> globalLengths, Map<String, ColumnStore> data, Map<Integer, Collection<String>> bucket2Entities) throws IOException {
		final OutputStream out = new GZIPOutputStream(new FileOutputStream(file));
		try (JsonGenerator generator = Jackson.BINARY_MAPPER.copy().enable(JsonGenerator.Feature.AUTO_CLOSE_TARGET).getFactory().createGenerator(out)) {

			log.debug("Writing header");

			generator.writeObject(header);

			log.debug("Writing data");

			for (Map.Entry<Integer, Collection<String>> bucketIds : bucket2Entities.entrySet()) {
				final Collection<String> entities = bucketIds.getValue();

				final Map<String, Integer> starts = Maps.filterKeys(globalStarts, entities::contains);
				final Map<String, Integer> lengths = Maps.filterKeys(globalLengths, entities::contains);

				final PreprocessedData preprocessedData = selectBucket(bucketIds.getKey(), starts, lengths, data);

				generator.writeObject(preprocessedData);
			}
		}
	}

	private static PreprocessedData selectBucket(int bucket, Map<String, Integer> localStarts, Map<String, Integer> localLengths, Map<String, ColumnStore> stores) {


		final IntList selectionStart = new IntArrayList();
		final IntList selectionLength = new IntArrayList();


		// First entity of Bucket starts at 0, the following are appended.
		final Object2IntMap<String> entityStarts = new Object2IntOpenHashMap<>();
		final Object2IntMap<String> entityEnds = new Object2IntOpenHashMap<>();


		int currentStart = 0;

		for (Map.Entry<String, Integer> entity2Start : localStarts.entrySet()) {
			final String entity = entity2Start.getKey();
			final int start = entity2Start.getValue();

			final int length = localLengths.get(entity);

			selectionStart.add(start);

			selectionLength.add(length);

			entityStarts.put(entity, currentStart);
			entityEnds.put(entity, currentStart + length);

			currentStart += length;
		}

		final Map<String, ColumnStore> selected = new HashMap<>();

		for (Map.Entry<String, ColumnStore> entry : stores.entrySet()) {
			final String name = entry.getKey();
			final ColumnStore store = entry.getValue();

			selected.put(name, store.select(selectionStart.toIntArray(), selectionLength.toIntArray()));
		}

		return new PreprocessedData(bucket, entityStarts, entityEnds, selected);
	}

	public synchronized String addPrimary(String primary) {
		return primaryColumn.addLine(primary);
	}

	public synchronized void addRow(String primaryId, PPColumn[] columns, Object[] outRow) {
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

			log.trace("Registering `{}` for Column[{}]", outRow[col], columns[col].getName());
			columns[col].getParser().addLine(outRow[col]);
		}

		//update stats
		rows++;
	}
}
