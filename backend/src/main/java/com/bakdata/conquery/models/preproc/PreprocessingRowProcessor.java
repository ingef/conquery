package com.bakdata.conquery.models.preproc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.DateReader;
import com.bakdata.conquery.util.io.ProgressBar;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.AbstractRowProcessor;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RowProcessor to ingest and parse a single CSV for Preprocessing.
 */
@Data
@RequiredArgsConstructor
@Slf4j
class PreprocessingRowProcessor extends AbstractRowProcessor {
	private final TableInputDescriptor input;
	private final Preprocessed result;
	private final Object2IntMap<Class<? extends Throwable>> exceptions;
	private final ProgressBar totalProgress;
	private final int maximumPrintedErrors;
	private final DateReader dateReader;
	private final ConqueryConfig config;
	private GroovyPredicate filter;
	private OutputDescription.Output primaryOut;
	private List<OutputDescription.Output> outputs;
	private long progress;
	private long errors;

	@Override
	public void processStarted(ParsingContext context) {
		final String[] headers = context.selectedHeaders();

		final Object2IntArrayMap<String> headerMap = new Object2IntArrayMap<>(headers.length);

		for (String header : headers) {
			headerMap.computeIfAbsent(header, (ToIntFunction<String>) context::indexOf);
		}

		// Compile filter.
		filter = input.createFilter(headers);

		// Instantiate Outputs based on descriptors (apply header positions)
		primaryOut = input.getPrimary().createForHeaders(headerMap, dateReader, config);

		outputs = new ArrayList<>();

		for (OutputDescription op : input.getOutput()) {
			outputs.add(op.createForHeaders(headerMap, dateReader, config));
		}
	}

	@Override
	public void rowProcessed(String[] row, ParsingContext context) {
		try {
			// Check if row shall be evaluated
			// This is explicitly NOT in a try-catch block as scripts may not fail, and we should not recover from faulty scripts.
			if (filter != null && !filter.filterRow(row)) {
				return;
			}

			try {
				final Object output = primaryOut.createOutput(row, result.getPrimaryColumn(), context.currentLine());

				if (output == null) {
					throw new NullPointerException("primaryId may not be null");
				}

				final int primaryId = (int) output;

				final int primary = result.addPrimary(primaryId);
				final PPColumn[] columns = result.getColumns();

				result.addRow(primary, columns, applyOutputs(outputs, columns, row, context.currentLine()));

			}
			catch (OutputDescription.OutputException e) {
				handleOutputException(row, e, context.currentLine());
			}
			catch (Exception e) {
				handleRawException(row, e, context.currentLine());
			}
		}
		finally {
			//report progress
			totalProgress.addCurrentValue(1);
		}
	}

	/**
	 * Apply each output for a single row. Returning all resulting values.
	 */
	private static Object[] applyOutputs(List<OutputDescription.Output> outputs, PPColumn[] columns, String[] row, long lineId)
			throws OutputDescription.OutputException {
		final Object[] outRow = new Object[outputs.size()];

		for (int index = 0; index < outputs.size(); index++) {
			final OutputDescription.Output out = outputs.get(index);

			try {
				final Parser parser = columns[index].getParser();

				final Object result = out.createOutput(row, parser, lineId);

				if (result == null) {
					continue;
				}

				outRow[index] = result;
			}
			catch (Exception e) {
				throw new OutputDescription.OutputException(out.getDescription(), e);
			}
		}
		return outRow;
	}

	/**
	 * OutputExceptions have more Information on their origin.
	 */
	private void handleOutputException(String[] row, OutputDescription.OutputException e, long lineId) {
		exceptions.put(e.getCause().getClass(), exceptions.getInt(e.getCause().getClass()) + 1);
		errors++;

		if (log.isTraceEnabled() || errors < maximumPrintedErrors) {
			log.warn("Failed to parse `{}` from line: {} content: {}", e.getSource(), lineId, row, e.getCause());
		}
		else if (errors == maximumPrintedErrors) {
			log.warn("More erroneous lines occurred. Only the first {}", maximumPrintedErrors);
		}
	}

	private void handleRawException(String[] row, Exception e, long lineId) {
		exceptions.put(e.getClass(), exceptions.getInt(e.getClass()) + 1);

		errors++;

		if (log.isTraceEnabled() || errors < maximumPrintedErrors) {
			log.warn("Failed to parse line: {} content: {}", lineId, row, e);
		}
		else if (errors == maximumPrintedErrors) {
			log.warn("More erroneous lines occurred. Only the first {} were printed", maximumPrintedErrors);
		}
	}

	@Override
	public void processEnded(ParsingContext context) {
		log.info("DONE reading file.");
	}
}
