package com.bakdata.conquery.models.preproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.DateReader;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.FileUtil;
import com.bakdata.conquery.util.io.LogUtil;
import com.bakdata.conquery.util.io.ProgressBar;
import com.google.common.base.Strings;
import com.google.common.io.CountingInputStream;
import com.univocity.parsers.csv.CsvParser;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
@UtilityClass
public class Preprocessor {

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
	 * Reads CSV file, per row extracts the primary key, then applies other transformations on each row, then compresses the data with {@link ColumnStore}.
	 */
	public static void preprocess(PreprocessingJob preprocessingJob, ProgressBar totalProgress, ConqueryConfig config) throws IOException {

		final File preprocessedFile = preprocessingJob.getPreprocessedFile();
		TableImportDescriptor descriptor = preprocessingJob.getDescriptor();

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

		log.info("PREPROCESSING START in {}", preprocessingJob);

		int errors = 0;

		final Preprocessed result = new Preprocessed(config, preprocessingJob);

		long lineId = 0;

		// Gather exception classes to get better overview of what kind of errors are happening.
		Object2IntMap<Class<? extends Throwable>> exceptions = new Object2IntArrayMap<>();
		exceptions.defaultReturnValue(0);


		for (int inputSource = 0; inputSource < descriptor.getInputs().length; inputSource++) {
			final TableInputDescriptor input = descriptor.getInputs()[inputSource];
			final File sourceFile = resolveSourceFile(input.getSourceFile(), preprocessingJob.getCsvDirectory(), preprocessingJob.getTag());

			final String name = String.format("%s:%s[%d/%s]", descriptor.toString(), descriptor.getTable(), inputSource, sourceFile.getName());
			ConqueryMDC.setLocation(name);

			if (!(sourceFile.exists() && sourceFile.canRead())) {
				throw new FileNotFoundException(sourceFile.getAbsolutePath());
			}

			CsvParser parser = null;


			try (CountingInputStream countingIn = new CountingInputStream(new FileInputStream(sourceFile))) {
				long progress = 0;

				CSVConfig csvSettings = config.getCsv();
				// Create CSV parser according to config, but overriding some behaviour.
				parser = csvSettings.withParseHeaders(true).withSkipHeader(false).createParser();

				parser.beginParsing(FileUtil.isGZipped(sourceFile) ? new GZIPInputStream(countingIn) : countingIn, csvSettings.getEncoding());

				final String[] headers = parser.getContext().parsedHeaders();

				final Object2IntArrayMap<String> headerMap = TableInputDescriptor.buildHeaderMap(headers);

				// Compile filter.
				final GroovyPredicate filter = input.createFilter(headers);


				DateReader dateReader = config.getLocale().getDateReader();
				final OutputDescription.Output primaryOut = input.getPrimary().createForHeaders(headerMap, dateReader, config);
				final List<OutputDescription.Output> outputs = new ArrayList<>();
				final PPColumn[] columns = result.getColumns();

				// Instantiate Outputs based on descriptors (apply header positions)
				for (OutputDescription op : input.getOutput()) {
					outputs.add(op.createForHeaders(headerMap, dateReader, config));
				}

				String[] row;

				// Read all CSV lines, apply Output transformations and add the to preprocessed.
				while ((row = parser.parseNext()) != null) {

					// Check if row shall be evaluated
					// This is explicitly NOT in a try-catch block as scripts may not fail and we should not recover from faulty scripts.
					if (filter != null && !filter.filterRow(row)) {
						continue;
					}

					try {
						int primaryId =
								(int) Objects.requireNonNull(primaryOut.createOutput(row, result.getPrimaryColumn(), lineId), "primaryId may not be null");


						final int primary = result.addPrimary(primaryId);
						final Object[] outRow = applyOutputs(outputs, columns, row, lineId);

						result.addRow(primary, columns, outRow);

					}
					catch (OutputDescription.OutputException e) {
						exceptions.put(e.getCause().getClass(), exceptions.getInt(e.getCause().getClass()) + 1);

						errors++;

						if (log.isTraceEnabled() || errors < config.getPreprocessor().getMaximumPrintedErrors()) {
							log.warn("Failed to parse `{}` from line: {} content: {}", e.getSource(), lineId, row, e.getCause());
						}
						else if (errors == config.getPreprocessor().getMaximumPrintedErrors()) {
							log.warn("More erroneous lines occurred. Only the first "
									 + config.getPreprocessor().getMaximumPrintedErrors()
									 + " were printed.");
						}

					}
					catch (Exception e) {
						exceptions.put(e.getClass(), exceptions.getInt(e.getClass()) + 1);

						errors++;

						if (log.isTraceEnabled() || errors < config.getPreprocessor().getMaximumPrintedErrors()) {
							log.warn("Failed to parse line: {} content: {}", lineId, row, e);
						}
						else if (errors == config.getPreprocessor().getMaximumPrintedErrors()) {
							log.warn("More erroneous lines occurred. Only the first "
									 + config.getPreprocessor().getMaximumPrintedErrors()
									 + " were printed.");
						}
					}
					finally {
						//report progress
						totalProgress.addCurrentValue(countingIn.getCount() - progress);
						progress = countingIn.getCount();
						lineId++;
					}
				}

			}
			finally {
				if (parser != null) {
					parser.stopParsing();
				}
			}
		}

		if (errors > 0) {
			log.warn("File `{}` contained {} faulty lines of ~{} total.", preprocessingJob, errors, lineId);
		}

		if (log.isWarnEnabled()) {
			exceptions.forEach((clazz, count) -> log.warn("Got {} `{}`", count, clazz.getSimpleName()));
		}

		result.write(tmp);

		if (errors > 0) {
			log.warn("Had {}% faulty lines ({} of ~{} lines)", String.format("%.2f", 100d * (double) errors / (double) lineId), errors, lineId);
		}

		if ((double) errors / (double) lineId > config.getPreprocessor().getFaultyLineThreshold()) {
			throw new RuntimeException("Too many faulty lines.");
		}


		//if successful move the tmp file to the target location
		FileUtils.moveFile(tmp, preprocessedFile);
		log.info("PREPROCESSING DONE in {}", preprocessingJob);
	}

	/**
	 * Apply each output for a single row. Returning all resulting values.
	 */
	private static Object[] applyOutputs(List<OutputDescription.Output> outputs, PPColumn[] columns, String[] row, long lineId)
			throws ParsingException, OutputDescription.OutputException {
		Object[] outRow = new Object[outputs.size()];

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
	 * Resolve a source file with tag appended if present, in csvDirectory.
	 */
	public static File resolveSourceFile(String fileName, Path csvDirectory, Optional<String> tag) {
		if (tag.isEmpty()) {
			return csvDirectory.resolve(fileName).toFile();
		}

		String name = fileName;
		final String suffix;

		if (name.endsWith(".csv.gz")) {
			name = name.substring(0, name.length() - ".csv.gz".length());
			suffix = ".csv.gz";
		}
		else if (name.endsWith(".csv")) {
			name = name.substring(0, name.length() - ".csv".length());
			suffix = ".csv";
		}
		else {
			throw new IllegalArgumentException("Unknown suffix for file " + name);
		}

		return csvDirectory.resolve(name + "." + tag.get() + suffix).toFile();
	}
}