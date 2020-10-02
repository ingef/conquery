package com.bakdata.conquery.models.preproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.csv.CsvIo;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.specific.string.MapTypeGuesser;
import com.bakdata.conquery.models.types.parser.specific.string.StringParser;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded.Encoding;
import com.bakdata.conquery.util.io.ConqueryFileUtil;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.LogUtil;
import com.bakdata.conquery.util.io.ProgressBar;
import com.google.common.base.Strings;
import com.google.common.io.CountingInputStream;
import com.jakewharton.byteunits.BinaryByteUnit;
import com.univocity.parsers.csv.CsvParser;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

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
				else {
					log.info("\tHASH OUTDATED");
				}
			} catch (Exception e) {
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
		if(Strings.isNullOrEmpty(tag)) {
			return file;
		}

		return new File(file.getParentFile(), file.getName().replaceAll(Pattern.quote(extension) + "$", String.format(".%s%s", tag, extension)));
	}


	/**
	 * Apply transformations in descriptor, then write them out to CQPP file for imports.
	 *
	 * Reads CSV file, per row extracts the primary key, then applies other transformations on each row, then compresses the data with {@link CType}.
	 */
	public static void preprocess(TableImportDescriptor descriptor, ProgressBar totalProgress, ConqueryConfig config) throws IOException {


		//create temporary folders and check for correct permissions
		final File preprocessedFile = descriptor.getInputFile().getPreprocessedFile();
		File tmp = ConqueryFileUtil.createTempFile(preprocessedFile.getName(), ConqueryConstants.EXTENSION_PREPROCESSED.substring(1));

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

		int errors = 0;

		final Preprocessed result = new Preprocessed(descriptor, config.getPreprocessor().getParsers());

		long lineId = 0;

		// Gather exception classes to get better overview of what kind of errors are happening.
		Object2IntMap<Class<? extends Throwable>> exceptions = new Object2IntArrayMap<>();
		exceptions.defaultReturnValue(0);


		try (HCFile outFile = new HCFile(tmp, true)) {
			for (int inputSource = 0; inputSource < descriptor.getInputs().length; inputSource++) {
				final TableInputDescriptor input = descriptor.getInputs()[inputSource];
				final File sourceFile = input.getSourceFile();

				final String name = String.format("%s:%s[%d/%s]", descriptor.toString(), descriptor.getTable(), inputSource, sourceFile.getName());
				ConqueryMDC.setLocation(name);

				if(!(sourceFile.exists() && sourceFile.canRead())){
					throw new FileNotFoundException(sourceFile.getAbsolutePath().toString());
				}

				CsvParser parser = null;


				try (CountingInputStream countingIn = new CountingInputStream(new FileInputStream(sourceFile))) {
					long progress = 0;

					CSVConfig csvSettings = config.getCsv();
					// Create CSV parser according to config, but overriding some behaviour.
					parser = new CsvParser(csvSettings.withParseHeaders(true).withSkipHeader(false).createCsvParserSettings());

					parser.beginParsing(CsvIo.isGZipped(sourceFile) ? new GZIPInputStream(countingIn) : countingIn, csvSettings.getEncoding());

					final String[] headers = parser.getContext().parsedHeaders();

					final Object2IntArrayMap<String> headerMap = TableInputDescriptor.buildHeaderMap(headers);

					// Compile filter.
					final GroovyPredicate filter = input.createFilter(headers);


					final OutputDescription.Output primaryOut = input.getPrimary().createForHeaders(headerMap);
					final List<OutputDescription.Output> outputs = new ArrayList<>();

					// Instantiate Outputs based on descriptors (apply header positions)
					for (OutputDescription op : input.getOutput()) {
						outputs.add(op.createForHeaders(headerMap));
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
							int primaryId = (int) Objects.requireNonNull(primaryOut.createOutput(row, result.getPrimaryColumn().getParser(), lineId), "primaryId may not be null");

							final int primary = result.addPrimary(primaryId);
							final PPColumn[] columns = result.getColumns();

							result.addRow(primary, columns, applyOutputs(outputs, columns, row, lineId));

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

				}finally {
					if(parser != null) {
						parser.stopParsing();
					}
				}
			}

			if (errors > 0) {
				log.warn("File `{}` contained {} faulty lines of ~{} total.", descriptor.getInputFile().getDescriptionFile(), errors, lineId);
			}

			if (log.isWarnEnabled()) {
				exceptions.forEach((clazz, count) -> log.warn("Got {} `{}`", count, clazz.getSimpleName()));
			}

			//find the optimal subtypes
			{
				log.info("finding optimal column types");

				StringParser parser = (StringParser) result.getPrimaryColumn().getParser();
				parser.setEncoding(Encoding.UTF8);
				result.getPrimaryColumn().setType(new MapTypeGuesser(parser).createGuess().getType());

				log.info(
						"\t{}.{}: {} -> {}",
						result.getName(),
						result.getPrimaryColumn().getName(),
						result.getPrimaryColumn().getParser(),
						result.getPrimaryColumn().getType()
				);

				for (PPColumn c : result.getColumns()) {
					log.trace("Compute best Subtype for  Column[{}] with {}", c.getName(), c.getParser());
					c.findBestType();
					log.trace("\t{}.{}: {} -> {}", result.getName(), c.getName(), c.getParser(), c.getType());
				}

				//estimate memory weight
				log.trace(
						"estimated total memory consumption: {} + n*{}",
						BinaryByteUnit.format(
								Arrays.stream(result.getColumns()).map(PPColumn::getType).mapToLong(CType::estimateMemoryConsumption).sum()
								+ result.getPrimaryColumn().getType().estimateMemoryConsumption()
						),
						BinaryByteUnit.format(
								Arrays.stream(result.getColumns()).map(PPColumn::getType).mapToLong(CType::estimateTypeSize).sum()
								+ result.getPrimaryColumn().getType().estimateTypeSize()
						)
				);

				for (PPColumn c : ArrayUtils.add(result.getColumns(), result.getPrimaryColumn())) {
					long typeConsumption = c.getType().estimateTypeSize();
					log.trace(
							"\t{}.{}: {}{}",
							result.getName(),
							c.getName(),
							BinaryByteUnit.format(c.getType().estimateMemoryConsumption()),
							typeConsumption == 0 ? "" : (" + n*" + BinaryByteUnit.format(typeConsumption))
					);
				}
			}

			result.write(outFile);
		}

		if(errors > 0){
			log.warn("Had {}% faulty lines ({} of ~{} lines)", String.format("%f.2", 100d * (double) errors / (double) lineId), errors, lineId);
		}

		if((double) errors / (double) lineId > config.getPreprocessor().getFaultyLineThreshold()){
			throw new RuntimeException("Too many faulty lines.");
		}


			//if successful move the tmp file to the target location
		FileUtils.moveFile(tmp, preprocessedFile);
		log.info("PREPROCESSING DONE in {}", descriptor.getInputFile().getDescriptionFile());
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
				final Parser<?> parser = columns[index].getParser();

				final Object result = out.createOutput(row, parser, lineId);

				if (result == null) {
					continue;
				}

				outRow[index] = result;
			}catch (Exception e){
				throw new OutputDescription.OutputException(out.getDescription(), e);
			}
		}
		return outRow;
	}
}