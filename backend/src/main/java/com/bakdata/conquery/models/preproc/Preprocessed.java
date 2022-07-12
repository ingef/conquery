package com.bakdata.conquery.models.preproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.models.preproc.parser.specific.StringParser;
import com.fasterxml.jackson.core.JsonGenerator;
import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
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
	private final IntList rowEntities = new IntArrayList();


	private long rows = 0;

	public Preprocessed(ConqueryConfig config, PreprocessingJob preprocessingJob) throws IOException {
		this.job = preprocessingJob;
		this.descriptor = preprocessingJob.getDescriptor();
		this.name = this.descriptor.getName();


		TableInputDescriptor input = this.descriptor.getInputs()[0];
		columns = new PPColumn[input.getWidth()];

		primaryColumn = (StringParser) MajorTypeId.STRING.createParser(config);

		values = new ColumnValues[columns.length];

		for (int index = 0; index < input.getWidth(); index++) {
			ColumnDescription columnDescription = input.getColumnDescription(index);
			columns[index] = new PPColumn(columnDescription.getName(), columnDescription.getType());

			final Parser parser = input.getOutput()[index].createParser(config);
			columns[index].setParser(parser);

			values[index] = parser.createColumnValues();
		}
	}


	public void write(File file) throws IOException {

		Int2IntMap entityStart = new Int2IntAVLTreeMap();
		Int2IntMap entityLength = new Int2IntAVLTreeMap();

		calculateEntitySpans(entityStart, entityLength);

		final IntSummaryStatistics statistics = entityLength.values().intStream().summaryStatistics();
		log.info("Statistics = {}", statistics);


		Map<String, ColumnStore> columnStores = combineStores(entityStart);

		Dictionary primaryDictionary = encodePrimaryDictionary();

		Map<String, Dictionary> dicts = collectDictionaries(columnStores);

		if (log.isTraceEnabled()) {
			for (Map.Entry<String, Dictionary> e : dicts.entrySet()) {
				String key = e.getKey();
				Dictionary dict = e.getValue();

				log.trace("{} of size {}", key, dict.size());

				for (int index = 0; index < dict.size(); index++) {
					//					log.trace("{} : {} => `{}`", key, index, StringEscapeUtils.escapeJava(new String(dict.getElement(index))));
										log.info("{} : {}", key, index);
				}
			}
		}

		log.debug("Writing Headers");

		int hash = descriptor.calculateValidityHash(job.getCsvDirectory(), job.getTag());

		PreprocessedHeader header = new PreprocessedHeader(
				descriptor.getName(),
				descriptor.getTable(),
				rows,
				columns,
				hash
		);

		final PreprocessedDictionaries dictionaries = new PreprocessedDictionaries(primaryDictionary, dicts);

		final PreprocessedData data = new PreprocessedData(entityStart, entityLength, columnStores);


		writePreprocessed(file, header, dictionaries, data);
	}

	private static void writePreprocessed(File file, PreprocessedHeader header, PreprocessedDictionaries dictionaries, PreprocessedData data)
			throws IOException {
		OutputStream out = new GZIPOutputStream(new FileOutputStream(file));
		try (JsonGenerator generator = Jackson.BINARY_MAPPER.copy()
															.enable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
															.getFactory()
															.createGenerator(out)) {

			log.debug("Writing header");

			generator.writeObject(header);

			log.debug("Writing Dictionaries");

			generator.writeObject(dictionaries);

			log.debug("Writing data");

			generator.writeObject(data);
		}
	}


	/**
	 * Calculate beginning and length of entities in output data.
	 */
	private void calculateEntitySpans(Int2IntMap entityStart, Int2IntMap entityLength) {

		// Count the number of events for the entity
		for (int entity : rowEntities) {
			final int curr = entityLength.getOrDefault(entity, 0);
			entityLength.put(entity, curr + 1);
		}

		// Lay out the entities in order, adding their length.
		int outIndex = 0;

		for (Int2IntMap.Entry entry : entityLength.int2IntEntrySet()) {
			entityStart.put(entry.getIntKey(), outIndex);
			outIndex += entry.getIntValue();
		}
	}

	/**
	 * Combine raw by-Entity data into column stores, appropriately formatted.
	 */
	@SuppressWarnings("rawtypes")
	private Map<String, ColumnStore> combineStores(Int2IntMap entityStart) {
		Map<String, ColumnStore> columnStores = Arrays.stream(columns)
													  .parallel()
													  .collect(Collectors.toMap(PPColumn::getName, PPColumn::findBestType));

		// This object can be huge!
		Int2ObjectMap<IntList> entityEvents = new Int2ObjectOpenHashMap<>(entityStart.size());

		for (int pos = 0, size = rowEntities.size(); pos < size; pos++) {
			int entity = rowEntities.getInt(pos);
			entityEvents.computeIfAbsent(entity, (ignored) -> new IntArrayList())
						.add(pos);
		}

		for (int colIdx = 0; colIdx < columns.length; colIdx++) {
			final PPColumn ppColumn = columns[colIdx];
			final ColumnValues columnValues = values[colIdx];

			//No need to preprocess the column further more, if it does not contain values, likely backed by a compound ColumnStore
			if (columnValues == null) {
				continue;
			}
			final ColumnStore store = columnStores.get(ppColumn.getName());

			entityStart.int2IntEntrySet()
					   .forEach(entry -> {
						   final int entity = entry.getIntKey();
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


	private Dictionary encodePrimaryDictionary() {
		log.debug("Encode primary Dictionary");

		primaryColumn.applyEncoding(StringTypeEncoded.Encoding.UTF8);

		MapDictionary primaryDict = new MapDictionary(Dataset.PLACEHOLDER, ConqueryConstants.PRIMARY_DICTIONARY);
		primaryColumn.getDecoded().forEach(primaryDict::add);

		return primaryDict;
	}

	private static Map<String, Dictionary> collectDictionaries(Map<String, ColumnStore> columnStores) {
		final Map<String, Dictionary> collect = new HashMap<>();
		for (Map.Entry<String, ColumnStore> entry : columnStores.entrySet()) {
			if (!(entry.getValue() instanceof StringStore)) {
				continue;
			}

			final Dictionary dictionary = ((StringStore) entry.getValue()).getUnderlyingDictionary();

			if (dictionary == null) {
				continue;
			}

			collect.put(entry.getKey(), dictionary);
		}

		return collect;
	}

	public synchronized int addPrimary(int primary) {
		primaryColumn.addLine(primary);
		return primary;
	}

	public synchronized void addRow(int primaryId, PPColumn[] columns, Object[] outRow) {
		int event = rowEntities.size();
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

			//			log.trace("Registering `{}` for Column[{}]", outRow[col], columns[col].getName());
			columns[col].getParser().addLine(outRow[col]);
		}

		//update stats
		rows++;
	}
}
