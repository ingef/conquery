package com.bakdata.conquery.models.preproc;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.parser.MajorTypeId;
import com.bakdata.conquery.models.events.parser.specific.string.MapTypeGuesser;
import com.bakdata.conquery.models.events.parser.specific.string.StringParser;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringType;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import lombok.Data;
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
	 * @implSpec this is ALWAYS {@link StringType}.
	 */
	private final StringParser primaryColumn;

	private final PPColumn[] columns;
	private final TableImportDescriptor descriptor;
	// by-column by-entity
	private final transient Table<Integer, Integer, List> entries;
	private long rows = 0;


	public Preprocessed(TableImportDescriptor descriptor, ParserConfig parserConfig) throws IOException {
		this.file = descriptor.getInputFile();
		this.name = descriptor.getName();
		this.descriptor = descriptor;

		TableInputDescriptor input = descriptor.getInputs()[0];
		columns = new PPColumn[input.getWidth()];

		primaryColumn = (StringParser) MajorTypeId.STRING.createParser(parserConfig);

		// pid and columns
		entries = HashBasedTable.create();

		for (int index = 0; index < input.getWidth(); index++) {
			ColumnDescription columnDescription = input.getColumnDescription(index);
			columns[index] = new PPColumn(columnDescription.getName(), columnDescription.getType());
			columns[index].setParser(columnDescription.getType().createParser(parserConfig));
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

		final IntSummaryStatistics statistics = entries.row(0).values().stream().mapToInt(List::size).summaryStatistics();

		log.info("Statistics = {}", statistics);

		Int2IntMap entityStart = new Int2IntAVLTreeMap();
		Int2IntMap entityLength = new Int2IntAVLTreeMap();

		calculateEntitySpans(entityStart, entityLength);

		Map<String, ColumnStore<?>> columnStores = combineStores();

		Dictionary primaryDictionary = encodePrimaryDictionary();

		Map<String, Dictionary> dicts = collectDictionaries(columnStores);

		writeHeader(outFile.writeHeader());

		writeData(outFile.writeContent(), entityStart, entityLength, columnStores, primaryDictionary, dicts);
	}

	/**
	 * Calculate beginning and end of
	 */
	private void calculateEntitySpans(Int2IntMap entityStart, Int2IntMap entityLength) {
		Map<Integer, List> values = entries.row(0);
		int start = 0;

		for (Integer entity : entries.columnKeySet()) {
			int length = values.get(entity).size();

			entityStart.put(entity.intValue(), start);
			entityLength.put(entity.intValue(), length);

			start += length;
		}
	}

	/**
	 * Combine raw by-Entity data into column stores, appropriately formatted.
	 */
	@SuppressWarnings("rawtypes")
	private Map<String, ColumnStore<?>> combineStores() {
		Map<String, ColumnStore<?>> columnStores = new HashMap<>(this.columns.length);

		for (int colIdx = 0; colIdx < columns.length; colIdx++) {
			final PPColumn ppColumn = this.columns[colIdx];

			final ColumnStore store = ppColumn.findBestType();

			Map<Integer, List> values = entries.row(colIdx);
			int start = 0;

			for (int entity : entries.columnKeySet()) {
				List entityValues = values.get(entity);
				int length = values.get(entity).size();

				for (int event = 0; event < length; event++) {
					store.set(start + event, entityValues.get(event));
				}

				start += length;
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

	private static Map<String, Dictionary> collectDictionaries(Map<String, ColumnStore<?>> columnStores) {
		final Map<String, Dictionary> collect = new HashMap<>();
		for (Map.Entry<String, ColumnStore<?>> entry : columnStores.entrySet()) {
			if (!(entry.getValue() instanceof StringType)) {
				continue;
			}

			final Dictionary dictionary = ((StringType) entry.getValue()).getUnderlyingDictionary();

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

	public static void writeData(OutputStream out1, Int2IntMap entityStart, Int2IntMap entityLength, Map<String, ColumnStore<?>> columnStores, Dictionary primaryDictionary, Map<String, Dictionary> dicts)
			throws IOException {
		try (OutputStream out = new BufferedOutputStream(new GzipCompressorOutputStream(out1))) {
			CONTAINER_WRITER.writeValue(out, new PreprocessedData(entityStart, entityLength, columnStores, primaryDictionary, dicts));
		}
	}

	public synchronized int addPrimary(int primary) {
		primaryColumn.addLine(primary);
		return primary;
	}

	public synchronized void addRow(int primaryId, PPColumn[] columns, Object[] outRow) {
		for (int col = 0; col < outRow.length; col++) {
			entries.row(col).computeIfAbsent(primaryId, (id) -> new ArrayList<>())
				   .add(outRow[col]);

			log.trace("Registering `{}` for Column[{}]", outRow[col], columns[col].getName());
			columns[col].getParser().addLine(outRow[col]);
		}

		//update stats
		rows++;
	}


}
