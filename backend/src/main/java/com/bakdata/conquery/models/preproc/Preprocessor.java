package com.bakdata.conquery.models.preproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.FileUtil;
import com.bakdata.conquery.util.io.LogUtil;
import com.bakdata.conquery.util.io.ProgressBar;
import com.google.common.base.Strings;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
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
	 * Reads CSV file, per row extracts the primary key, then applies other transformations on each row, then compresses the data with {@link com.bakdata.conquery.models.events.stores.root.ColumnStore}.
	 */
	public static void preprocess(PreprocessingJob preprocessingJob, ProgressBar progressBar, ConqueryConfig config) throws IOException {

		final File preprocessedFile = preprocessingJob.getPreprocessedFile();
		final TableImportDescriptor descriptor = preprocessingJob.getDescriptor();

		// Create temp file that will be moved when finished (we ensure the same file system, to avoid unnecessary copying)
		final File tmp = new File(preprocessedFile.getParentFile(), preprocessedFile.getName() + ".tmp");

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

		// Preprocessed data is collected into this.
		final Preprocessed result = new Preprocessed(config, preprocessingJob);

		long lines = 0;
		long errors = 0;

		// Gather exception classes to get better overview of what kind of errors are happening.
		final Object2IntMap<Class<? extends Throwable>> exceptions = new Object2IntArrayMap<>();
		exceptions.defaultReturnValue(0);

		for (final TableInputDescriptor input : descriptor.getInputs()) {
			final File sourceFile = resolveSourceFile(input.getSourceFile(), preprocessingJob.getCsvDirectory(), preprocessingJob.getTag());

			final String name = String.format("%s:%s[%s]", descriptor, descriptor.getTable(), sourceFile.getName());

			ConqueryMDC.setLocation(name);

			if (!(sourceFile.exists() && sourceFile.canRead())) {
				throw new FileNotFoundException(sourceFile.getAbsolutePath());
			}

			CsvParser parser = null;
			final PreprocessingRowProcessor processor =
					new PreprocessingRowProcessor(input, result, exceptions, progressBar, config.getPreprocessor().getMaximumPrintedErrors(), config.getLocale()
																																					.getDateReader(), config);

			try (InputStream inputStream = new FileInputStream(sourceFile)) {

				final CSVConfig csvSettings = config.getCsv();

				// Create CSV parser according to config, but overriding some behaviour.
				final CsvParserSettings parserSettings =
						csvSettings.withParseHeaders(true)
								   .withSkipHeader(false)
								   .createCsvParserSettings();

				// Only read what we need.
				parserSettings.selectFields(input.getRequiredHeaders().toArray(new String[0]));

				parserSettings.setProcessor(processor);

				parser = new CsvParser(parserSettings);

				parser.parse(FileUtil.isGZipped(sourceFile) ? new GZIPInputStream(inputStream) : inputStream, csvSettings.getEncoding());

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
			log.warn("File `{}` contained {} faulty lines of {} total.", preprocessingJob, errors, lines);
			log.warn("Had {}% faulty lines ({} of ~{} lines)", String.format("%f.2", 100d * (double) errors / (double) lines), errors, lines);
		}

		if (log.isWarnEnabled()) {
			exceptions.forEach((clazz, count) -> log.warn("Got {} `{}`", count, clazz.getSimpleName()));
		}

		result.write(tmp);

		if (errors > 0) {
			log.warn("Had {}% faulty lines ({} of ~{} lines)", String.format("%.2f", 100d * (double) errors / (double) lines), errors, lines);
		}

		if ((double) errors / (double) lines > config.getPreprocessor().getFaultyLineThreshold()) {
			throw new RuntimeException("Too many faulty lines.");
		}


		//if successful move the tmp file to the target location
		FileUtils.moveFile(tmp, preprocessedFile);
		log.info("PREPROCESSING DONE in {}", preprocessingJob);
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