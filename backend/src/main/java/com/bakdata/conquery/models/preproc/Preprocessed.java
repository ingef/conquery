package com.bakdata.conquery.models.preproc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.parser.specific.string.StringParser;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

@Data
@Slf4j
public class Preprocessed {

	private final InputFile file;
	private final String name;
	private final PPColumn primaryColumn;
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


		primaryColumn = new PPColumn(input.getPrimary().getColumnDescription().getName());
		primaryColumn.setParser(input.getPrimary().getColumnDescription().getType().createParser(parserConfig));

		if (!(primaryColumn.getParser() instanceof StringParser)) {
			throw new IllegalStateException("The primary column must be an ENTITY_ID or STRING column");
		}

		// pid and columns
		entries = HashBasedTable.create();

		for (int index = 0; index < input.getWidth(); index++) {
			ColumnDescription columnDescription = input.getColumnDescription(index);
			columns[index] = new PPColumn(columnDescription.getName());
			columns[index].setParser(columnDescription.getType().createParser(parserConfig));
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public void write(HCFile outFile) throws IOException {
		if (!outFile.isWrite()) {
			throw new IllegalArgumentException("outfile was opened in read-only mode.");
		}

		// Write content to file
		Import imp = Import.createForPreprocessing(descriptor.getTable(), descriptor.getName(), columns);

		final IntSummaryStatistics statistics = entries.row(0).values().stream().mapToInt(List::size).summaryStatistics();

		final int nEvents = (int) statistics.getSum();

		log.info("Statistics = {}", statistics);

		Int2IntMap entityStart = new Int2IntAVLTreeMap();
		Int2IntMap entityLength = new Int2IntAVLTreeMap();

		ColumnStore[] columnValues = new ColumnStore[columns.length];

		ImportColumn[] impColumns = imp.getColumns();

		for (int colIdx = 0; colIdx < impColumns.length; colIdx++) {
			final PPColumn ppColumn = columns[colIdx];

			final ColumnStore store = ppColumn.getType();

			Map<Integer, List> values = entries.row(colIdx);
			int start = 0;

			for (Integer entity : entries.columnKeySet()) {
				List entityValues = values.get(entity);
				int length = values.get(entity).size();

				entityStart.put(entity.intValue(), start);

				for (int event = 0; event < length; event++) {
					store.set(start + event, entityValues.get(event));
				}

				entityLength.put(entity.intValue(), length);

				start += length;
			}

			columnValues[colIdx] = store;
		}


		try (OutputStream out = new GzipCompressorOutputStream(outFile.writeContent())) {
			Jackson.BINARY_MAPPER.writerFor(DataContainer.class)
								 .writeValue(out, new DataContainer(entityStart, entityLength, columnValues));
		}

		// Then write headers.
		try (OutputStream out = outFile.writeHeader()) {
			int hash = descriptor.calculateValidityHash();

			PreprocessedHeader header = new PreprocessedHeader(
					descriptor.getName(),
					descriptor.getTable(),
					imp.getSuffix(),
					rows,
					primaryColumn,
					columns,
					hash
			);

			try {
				Jackson.BINARY_MAPPER.writeValue(out, header);
				primaryColumn.getType().writeHeader(out);
				for (PPColumn col : columns) {
					col.getType().writeHeader(out);
				}
				out.flush();
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to serialize header " + header, e);
			}
		}
	}

	public synchronized int addPrimary(int primary) {
		primaryColumn.getParser().addLine(primary);
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



	@Data
	@AllArgsConstructor(onConstructor_ = @JsonCreator)
	public static class DataContainer {
		private final Map<Integer, Integer> starts;
		private final Map<Integer, Integer> lengths;
		private final ColumnStore[] values;
	}


}
