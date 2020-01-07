package com.bakdata.conquery.models.preproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.csv.CsvIo;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
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
import com.google.common.io.CountingInputStream;
import com.jakewharton.byteunits.BinaryByteUnit;
import com.univocity.parsers.csv.CsvParser;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
@UtilityClass
public class Preprocessor {

	public static long getTotalCsvSize(TableImportDescriptor descriptor) {
		long totalCsvSize = 0;
		for (Input input : descriptor.getInputs()) {
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

				PreprocessedHeader header = Jackson.BINARY_MAPPER.readValue(is, PPHeader.class);

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
	 * Apply transformations in descriptor, then write them out to CQPP file for imports.
	 *
	 * Reads CSV file, per row extracts the primary key, then applies other transformations on each row, then compresses the data with {@link CType}.
	 */
	public static void preprocess(TableImportDescriptor descriptor, ProgressBar totalProgress) throws IOException, JSONException, ParsingException {


		//create temporary folders and check for correct permissions
		File tmp = ConqueryFileUtil.createTempFile(descriptor
														   .getInputFile()
														   .getPreprocessedFile()
														   .getName(), ConqueryConstants.EXTENSION_PREPROCESSED.substring(1));
		if (!Files.isWritable(tmp.getParentFile().toPath())) {
			throw new IllegalArgumentException("No write permission in " + LogUtil.printPath(tmp.getParentFile()));
		}
		if (!Files.isWritable(descriptor.getInputFile().getPreprocessedFile().toPath().getParent())) {
			throw new IllegalArgumentException("No write permission in " + LogUtil.printPath(descriptor
																									 .getInputFile()
																									 .getPreprocessedFile()
																									 .toPath()
																									 .getParent()));
		}
		//delete target file if it exists
		if (descriptor.getInputFile().getPreprocessedFile().exists()) {
			FileUtils.forceDelete(descriptor.getInputFile().getPreprocessedFile());
		}

		log.info("PREPROCESSING START in {}", descriptor.getInputFile().getDescriptionFile());

		final AtomicLong errorCounter = new AtomicLong(0);

		final Preprocessed result = new Preprocessed(descriptor);

		long lineId = 0;

		try (HCFile outFile = new HCFile(tmp, true)) {
			for (int inputSource = 0; inputSource < descriptor.getInputs().length; inputSource++) {

				final String name = String.format("%s:%s[%d]", descriptor.toString(), descriptor.getTable(), inputSource);
				ConqueryMDC.setLocation(name);

				final Input input = descriptor.getInputs()[inputSource];

				try (CountingInputStream countingIn = new CountingInputStream(new FileInputStream(input.getSourceFile()))) {
					long progress = 0;

					// Create CSV parser according to config, but overriding some behaviour.
					final CsvParser parser =
							new CsvParser(ConqueryConfig.getInstance().getCsv().withParseHeaders(true).withSkipHeader(false).createCsvParserSettings());
					// TODO wrap with pre-fetching iterator?

					parser.beginParsing(CsvIo.isGZipped(input.getSourceFile()) ? new GZIPInputStream(countingIn) : countingIn);

					final String[] headers = parser.getContext().parsedHeaders();

					final Object2IntArrayMap<String> headerMap = Input.buildHeaderMap(headers);

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
						try {

							int primaryId = (int) Objects.requireNonNull(primaryOut.createOutput(row, result.getPrimaryColumn().getParser(), lineId), "primaryId may not be null");

							if (filter != null && !filter.filterRow(row)) {
								continue;
							}

							final int primary = result.addPrimary(primaryId);
							final PPColumn[] columns = result.getColumns();

							result.addRow(primary, columns, applyOutputs(outputs, columns, row, lineId));

						}
						catch (ParsingException e) {

							long errors = errorCounter.getAndIncrement();

							if (log.isTraceEnabled() || errors < ConqueryConfig.getInstance().getPreprocessor().getMaximumPrintedErrors()) {
								log.warn("Failed to parse primary from line:" + lineId + " content:" + Arrays.toString(row), e);
							}
							else if (errors == ConqueryConfig.getInstance().getPreprocessor().getMaximumPrintedErrors()) {
								log.warn("More erroneous lines occurred. Only the first "
										 + ConqueryConfig.getInstance().getPreprocessor().getMaximumPrintedErrors()
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

					parser.stopParsing();
				}
			}

			//find the optimal subtypes
			{
				log.info("finding optimal column types");
				log.info(
						"\t{}.{}: {} -> {}",
						result.getName(),
						result.getPrimaryColumn().getName(),
						result.getPrimaryColumn().getParser(),
						result.getPrimaryColumn().getType()
				);

				StringParser parser = (StringParser) result.getPrimaryColumn().getParser();
				parser.setEncoding(Encoding.UTF8);
				result.getPrimaryColumn().setType(new MapTypeGuesser(parser).createGuess().getType());

				for (PPColumn c : result.getColumns()) {
					c.findBestType();
					log.info("\t{}.{}: {} -> {}", result.getName(), c.getName(), c.getParser(), c.getType());
				}

				//estimate memory weight
				log.info(
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
					log.info(
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

		if (errorCounter.get() > 0 && log.isWarnEnabled()) {
			log.warn("File `{}` contained {} faulty lines of ~{} total.", descriptor.getInputFile().getDescriptionFile(), errorCounter.get(), lineId);
		}

		//if successful move the tmp file to the target location
		FileUtils.moveFile(tmp, descriptor.getInputFile().getPreprocessedFile());
		log.info("PREPROCESSING DONE in {}", descriptor.getInputFile().getDescriptionFile());
	}

	/**
	 * Apply each output for a single row. Returning all resulting values.
	 */
	private static Object[] applyOutputs(List<OutputDescription.Output> outputs, PPColumn[] columns, String[] row, long lineId) throws ParsingException {
		Object[] outRow = new Object[outputs.size()];

		for (int c = 0; c < outputs.size(); c++) {

			final OutputDescription.Output out = outputs.get(c);
			final Parser<?> parser = columns[c].getParser();

			final Object result = out.createOutput(row, parser, lineId);

			if (result == null) {
				continue;
			}

			if (outRow == null) {
				outRow = new Object[outputs.size()];
			}

			outRow[c] = result;
		}
		return outRow;
	}
}