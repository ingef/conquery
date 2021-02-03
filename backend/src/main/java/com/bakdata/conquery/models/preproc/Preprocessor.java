package com.bakdata.conquery.models.preproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.csv.CsvIo;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.parser.Parser;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.LogUtil;
import com.bakdata.conquery.util.io.ProgressBar;
import com.google.common.base.Strings;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.AbstractRowProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
@UtilityClass
public class Preprocessor {

	public static long getTotalCsvSize(TableImportDescriptor descriptor) {
		long totalCsvSize = 0;
		for (TableInputDescriptor input : descriptor.getInputs()) {
			totalCsvSize += input.getSourceFile().length();
		}

		return totalCsvSize;
	}

	public static boolean requiresProcessing(TableImportDescriptor descriptor) {
		ConqueryMDC.setLocation(descriptor.toString());
		if (descriptor.getInputFile().getPreprocessedFile().exists()) {

			log.info("EXISTS ALREADY");

			int currentHash = descriptor.calculateValidityHash();

			try (HCFile outFile = new HCFile(descriptor.getInputFile().getPreprocessedFile(), false);
				 InputStream is = outFile.readHeader()) {

				PreprocessedHeader header = Jackson.BINARY_MAPPER.readValue(is, PreprocessedHeader.class);

				if (header.getValidityHash() == currentHash) {
					log.info("\tHASH STILL VALID");
					return false;
				}
				log.info("\tHASH OUTDATED");
			}
			catch (Exception e) {
				log.error("\tHEADER READING FAILED", e);
				return false;
			}
		}
		else {
			log.info("DOES NOT EXIST");
		}

		return true;
	}

	/**
	 * Create version of file-name with tag.
	 */
	public static File getTaggedVersion(File file, String tag, String extension) {
		if (Strings.isNullOrEmpty(tag)) {
			return file;
		}

		return new File(file.getParentFile(), file.getName().replaceAll(Pattern.quote(extension) + "$", String.format(".%s%s", tag, extension)));
	}


	/**
	 * Apply transformations in descriptor, then write them out to CQPP file for imports.
	 * <p>
	 * Reads CSV file, per row extracts the primary key, then applies other transformations on each row, then compresses the data with {@link com.bakdata.conquery.models.events.stores.ColumnStore}.
	 */
	public static void preprocess(TableImportDescriptor descriptor, ProgressBar progressBar, ConqueryConfig config) throws IOException {

		final File preprocessedFile = descriptor.getInputFile().getPreprocessedFile();

		// Create temp file that will be moved when finished (we ensure the same file system, to avoid unnecessary copying)
		File tmp = new File(preprocessedFile.getParentFile(), preprocessedFile.getName() + ".tmp");

		// Ensures deletion on failure
		tmp.deleteOnExit();

		if (!Files.isWritable(tmp.getParentFile().toPath())) {
			throw new IllegalArgumentException("No write permission in " + LogUtil.printPath(tmp.getParentFile()));
		}

		if (!Files.isWritable(preprocessedFile.toPath().getParent())) {
			throw new IllegalArgumentException("No write permission in " + LogUtil.printPath(preprocessedFile
																									 .toPath()
																									 .getParent()));
		}

		//delete target file if it exists
		if (preprocessedFile.exists()) {
			FileUtils.forceDelete(preprocessedFile);
		}

		log.info("PREPROCESSING START in {}", descriptor.getInputFile().getDescriptionFile());

		// Preprocessed data is collected into this.
		final Preprocessed result = new Preprocessed(descriptor, config.getPreprocessor().getParsers());

		long lines = 0;
		long errors = 0;

		// Gather exception classes to get better overview of what kind of errors are happening.
		Object2IntMap<Class<? extends Throwable>> exceptions = new Object2IntArrayMap<>();
		exceptions.defaultReturnValue(0);

		for (final TableInputDescriptor input : descriptor.getInputs()) {
			final File sourceFile = input.getSourceFile();

			final String name = String.format("%s:%s[%s]", descriptor.toString(), descriptor.getTable(), sourceFile.getName());

			ConqueryMDC.setLocation(name);

			if (!(sourceFile.exists() && sourceFile.canRead())) {
				throw new FileNotFoundException(sourceFile.getAbsolutePath());
			}

			CsvParser parser = null;
			final PreprocessingRowProcessor processor =
					new PreprocessingRowProcessor(input, result, exceptions, progressBar, config.getPreprocessor().getMaximumPrintedErrors());

			try (InputStream inputStream = new FileInputStream(sourceFile)) {

				CSVConfig csvSettings = config.getCsv();

				// Create CSV parser according to config, but overriding some behaviour.
				final CsvParserSettings parserSettings =
						csvSettings.withParseHeaders(true)
								   .withSkipHeader(false)
								   .createCsvParserSettings();

				// Only read what we need.
				parserSettings.selectFields(input.getRequiredHeaders().toArray(new String[0]));

				parserSettings.setProcessor(processor);

				parser = new CsvParser(parserSettings);

				parser.parse(CsvIo.isGZipped(sourceFile) ? new GZIPInputStream(inputStream) : inputStream, csvSettings.getEncoding());

			}
			finally {
				if (parser != null) {
					parser.stopParsing();
					lines += parser.getContext().currentLine();
					errors += processor.getErrors();
				}
			}
		}

		if (errors > 0) {
			log.warn("File `{}` contained {} faulty lines of {} total.", descriptor.getInputFile().getDescriptionFile(), errors, lines);
			log.warn("Had {}% faulty lines ({} of ~{} lines)", String.format("%f.2", 100d * (double) errors / (double) lines), errors, lines);
		}

		if (log.isWarnEnabled()) {
			exceptions.forEach((clazz, count) -> log.warn("Got {} `{}`", count, clazz.getSimpleName()));
		}

		if ((double) errors / (double) lines > config.getPreprocessor().getFaultyLineThreshold()) {
			throw new RuntimeException("Too many faulty lines.");
		}

		try (HCFile outFile = new HCFile(tmp, true)) {
			result.write(outFile);
		}

		//if successful move the tmp file to the target location
		FileUtils.moveFile(tmp, preprocessedFile);
		log.info("PREPROCESSING DONE in {}", descriptor.getInputFile().getDescriptionFile());
	}

	/**
	 * Apply each output for a single row. Returning all resulting values.
	 */
	private static Object[] applyOutputs(List<OutputDescription.Output> outputs, PPColumn[] columns, String[] row, long lineId)
			throws OutputDescription.OutputException {
		Object[] outRow = new Object[outputs.size()];

		for (int index = 0; index < outputs.size(); index++) {
			final OutputDescription.Output out = outputs.get(index);

			try {
				final Parser<?> parser = columns[index].getParser();

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

	@Data
	@RequiredArgsConstructor
	private static class PreprocessingRowProcessor extends AbstractRowProcessor {
		private final TableInputDescriptor input;
		private final Preprocessed result;
		private final Object2IntMap<Class<? extends Throwable>> exceptions;
		private final ProgressBar totalProgress;
		private final int maximumPrintedErrors;

		private GroovyPredicate filter;
		private OutputDescription.Output primaryOut;
		private List<OutputDescription.Output> outputs;

		private long progress =  0;
		private long errors = 0;


		@Override
		public void processStarted(ParsingContext context) {
			final String[] headers = context.selectedHeaders();

			final Object2IntArrayMap<String> headerMap = new Object2IntArrayMap<>(headers.length);

			for (String header : headers) {
				headerMap.computeIfAbsent(header, (ToIntFunction<String>) context::indexOf);
			}

			// Compile filter.
			filter = input.createFilter(headers);

			primaryOut = input.getPrimary().createForHeaders(headerMap);
			outputs = new ArrayList<>();

			// Instantiate Outputs based on descriptors (apply header positions)
			for (OutputDescription op : input.getOutput()) {
				outputs.add(op.createForHeaders(headerMap));
			}
		}

		@Override
		public void rowProcessed(String[] row, ParsingContext context) {
			try {
				// Check if row shall be evaluated
				// This is explicitly NOT in a try-catch block as scripts may not fail and we should not recover from faulty scripts.
				if (filter != null && !filter.filterRow(row)) {
					return;
				}

				try {
					int primaryId =
							(int) Objects.requireNonNull(
									primaryOut.createOutput(row, result.getPrimaryColumn(), context.currentLine()),
									"primaryId may not be null"
							);

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
			log.info("Done reading file.");
		}
	}
}