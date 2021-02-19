package com.bakdata.conquery.models.preproc;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.parser.ColumnValues;
import com.bakdata.conquery.models.events.parser.MajorTypeId;
import com.bakdata.conquery.models.events.parser.Parser;
import com.bakdata.conquery.models.events.parser.specific.StringParser;
import com.bakdata.conquery.models.events.parser.specific.string.MapTypeGuesser;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

@Data
@Slf4j
public class Preprocessed {


	private static final ObjectReader CONTAINER_READER = Jackson.BINARY_MAPPER.readerFor(PreprocessedData.class);
	private static final ObjectWriter CONTAINER_WRITER = Jackson.BINARY_MAPPER.writerFor(PreprocessedData.class);
	private final InputFile file;
	private final String name;
	/**
	 * @implSpec this is ALWAYS {@link StringStore}.
	 */
	private final StringParser primaryColumn;

	private final PPColumn[] columns;
	private final TableImportDescriptor descriptor;

	// by-column
	private final ColumnValues[] values;

	// by-entity
	private final Int2ObjectMap<EntityPositions> entries;
	private final IntSet entities = new IntAVLTreeSet();

	private long rows = 0;

	public Preprocessed(TableImportDescriptor descriptor, ParserConfig parserConfig) throws IOException {
		this.file = descriptor.getInputFile();
		this.name = descriptor.getName();
		this.descriptor = descriptor;

		TableInputDescriptor input = descriptor.getInputs()[0];
		columns = new PPColumn[input.getWidth()];

		primaryColumn = (StringParser) MajorTypeId.STRING.createParser(parserConfig);

		// pid and columns
		entries = new Int2ObjectAVLTreeMap<>();
		values = new ColumnValues[columns.length];

		for (int index = 0; index < input.getWidth(); index++) {
			ColumnDescription columnDescription = input.getColumnDescription(index);
			columns[index] = new PPColumn(columnDescription.getName(), columnDescription.getType());
			columns[index].setParser(columnDescription.getType().createParser(parserConfig));

			final Parser parser = columns[index].getParser();
			values[index] = parser.createColumnValues(parserConfig);
		}
	}

	/**
	 * Read the data section of a CQPP file.
	 */
	public static PreprocessedData readContainer(InputStream in) throws IOException {
		return CONTAINER_READER.readValue(new GZIPInputStream(in));
	}

	public void write(HCFile outFile) throws IOException {
		if (!outFile.isWrite()) {
			throw new IllegalArgumentException("outfile was opened in read-only mode.");
		}

		// TODO implement statistics
		//		final IntSummaryStatistics statistics = entries.row(0).values().stream().mapToInt(List::size).summaryStatistics();
		//
		//		log.info("Statistics = {}", statistics);

		Int2IntMap entityStart = new Int2IntAVLTreeMap();
		Int2IntMap entityLength = new Int2IntAVLTreeMap();

		calculateEntitySpans(entityStart, entityLength);

		Map<String, ColumnStore> columnStores = combineStores();

		Dictionary primaryDictionary = encodePrimaryDictionary();

		Map<String, Dictionary> dicts = collectDictionaries(columnStores);

		log.debug("Writing Headers");

		writeHeader(outFile.writeHeader());

		log.debug("Writing data");

		writeData(outFile.writeContent(), entityStart, entityLength, columnStores, primaryDictionary, dicts);

		for (ColumnValues columnValues : values) {
			columnValues.close();
		}
	}

	/**
	 * Calculate beginning and end of
	 */
	private void calculateEntitySpans(Int2IntMap entityStart, Int2IntMap entityLength) {
		Int2ObjectMap<EntityPositions> values = entries;
		int start = 0;

		for (int entity : entities) {
			final EntityPositions events = values.get(entity);

			if(events == null){
				continue; //TODO is this no data or just no data for column?
			}

			final int length = events.length();

			entityStart.put(entity, start);
			entityLength.put(entity, length);

			start += length;
		}
	}

	/**
	 * Combine raw by-Entity data into column stores, appropriately formatted.
	 */
	@SuppressWarnings("rawtypes")
	private Map<String, ColumnStore> combineStores() {
		Map<String, ColumnStore> columnStores = new HashMap<>(this.columns.length);

		for (int colIdx = 0; colIdx < columns.length; colIdx++) {
			final PPColumn ppColumn = this.columns[colIdx];

			final ColumnStore store = ppColumn.findBestType();
			Int2ObjectMap<EntityPositions> indices = entries;

			final ColumnValues columnValues = values[colIdx];

			int start = 0;

			for (int entity : entities) {

				final EntityPositions entityIndices = indices.get(entity);

				// TODO is that right?
				if(entityIndices == null){
					continue;
				}

				for (int inIndex : entityIndices) {

					if (columnValues.isNull(inIndex)) {
						store.setNull(start);
					}
					else {
						final Object raw = columnValues.get(inIndex);
						ppColumn.getParser().setValue(store, start, raw);
					}

					start++;
				}
			}

			columnStores.put(ppColumn.getName(), store);
		}
		return columnStores;
	}

	private Dictionary encodePrimaryDictionary() {
		log.info("finding optimal column types");

		primaryColumn.applyEncoding(StringTypeEncoded.Encoding.UTF8);

		final Dictionary primaryDictionary = new MapTypeGuesser(primaryColumn).createGuess().getType().getUnderlyingDictionary();
		log.info("\tPrimaryColumn -> {}", primaryDictionary);
		return primaryDictionary;
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

	private void writeHeader(OutputStream out) {
		int hash = descriptor.calculateValidityHash();

		PreprocessedHeader header = new PreprocessedHeader(
				descriptor.getName(),
				descriptor.getTable(),
				rows,
				this.columns,
				hash
		);

		try {
			Jackson.BINARY_MAPPER.writeValue(out, header);
			out.flush();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to serialize header " + header, e);
		}
	}

	public static void writeData(OutputStream out1, Int2IntMap entityStart, Int2IntMap entityLength, Map<String, ColumnStore> columnStores, Dictionary primaryDictionary, Map<String, Dictionary> dicts)
			throws IOException {
		try (OutputStream out = new BufferedOutputStream(new GzipCompressorOutputStream(out1))) {
			CONTAINER_WRITER.writeValue(out, new PreprocessedData(entityStart, entityLength, columnStores, primaryDictionary, dicts));
		}
	}

	public synchronized int addPrimary(int primary) {
		primaryColumn.addLine(primary);
		entities.add(primary);
		return primary;
	}

	public synchronized void addRow(int primaryId, PPColumn[] columns, Object[] outRow) {
		int event = -1;

		for (int col = 0; col < outRow.length; col++) {
			final int idx = values[col].add(outRow[col]);

			if(event == -1){
				event = idx;
			}

			if(idx != event){
				log.error("Columns are not aligned");
			}

			log.trace("Registering `{}` for Column[{}]", outRow[col], columns[col].getName());
			columns[col].getParser().addLine(outRow[col]);
		}

		final int _event = event;

		entries.computeIfAbsent(primaryId, (id) -> new EntityPositions(_event)).add(event);


		//update stats
		rows++;
	}

	/**
	 * Offset encoded positions, in the assumption that entity values are stored close to each other.
	 */
	@RequiredArgsConstructor
	private static class EntityPositions implements IntIterable {
		private final int start;
		private final IntList offsets = new IntArrayList();

		public void add(int event){
			Preconditions.checkArgument(event >= start, "Events must be added in order");
			offsets.add(event);
		}

		public int length() {
			return offsets.size();
		}


		@Override
		public IntIterator iterator() {
			return offsets.iterator();
		}
	}


}
