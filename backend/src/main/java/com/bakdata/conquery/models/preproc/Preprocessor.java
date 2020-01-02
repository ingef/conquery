package com.bakdata.conquery.models.preproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.csv.CsvIo;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.outputs.AutoOutput;
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

	public static long getTotalCsvSize(ImportDescriptor descriptor) {
		long totalCsvSize = 0;
		for (Input input : descriptor.getInputs()) {
			totalCsvSize += input.getSourceFile().length();
		}

		return totalCsvSize;
	}

	public static boolean requiresProcessing(ImportDescriptor descriptor) {
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

	public static void preprocess(ProgressBar totalProgress, ImportDescriptor descriptor) throws IOException, JSONException, ParsingException {

		ConqueryMDC.setLocation(descriptor.toString());

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

					final CsvParser
							parser =
							new CsvParser(ConqueryConfig.getInstance().getCsv().withParseHeaders(true).withSkipHeader(false).createCsvParserSettings());
					// TODO wrap with prefetching iterator

					parser.beginParsing(CsvIo.isGZipped(input.getSourceFile()) ? new GZIPInputStream(countingIn) : countingIn);

					final String[] headers = parser.getContext().parsedHeaders();


					final Object2IntArrayMap<String> headerMap = Input.buildHeaderMap(headers);

					final OutputDescription.Output primary = input.getPrimary().createForHeaders(headerMap);
					final List<OutputDescription.Output> outputs = new ArrayList<>();

					for (OutputDescription op : input.getOutput()) {
						outputs.add(op.createForHeaders(headerMap));
					}

					final GroovyPredicate filter = input.createFilter(headers);

					String[] row;

					while ((row = parser.parseNext()) != null) {
						try {

							Integer primaryId = parsePrimary((StringParser) result.getPrimaryColumn().getParser(), row, lineId, inputSource, primary);

							if (primaryId == null) {
								continue;
							}

							parseRow(result.addPrimary(primaryId), result.getColumns(), row, input, lineId, result, inputSource, outputs, filter);

						} catch (ParsingException e) {

							long errors = errorCounter.getAndIncrement();

							if (log.isTraceEnabled() || errors < ConqueryConfig.getInstance().getPreprocessor().getMaximumPrintedErrors()) {
								log.warn("Failed to parse primary from line:" + lineId + " content:" + Arrays.toString(row), e);
							}
							else if (errors == ConqueryConfig.getInstance().getPreprocessor().getMaximumPrintedErrors()) {
								log.warn("More erroneous lines occurred. Only the first "
										 + ConqueryConfig.getInstance().getPreprocessor().getMaximumPrintedErrors()
										 + " were printed.");
							}
						} finally {
							//report progress
							totalProgress.addCurrentValue(countingIn.getCount() - progress);
							progress = countingIn.getCount();
							lineId++;
						}
					}

					parser.stopParsing();

					if (input.checkAutoOutput()) {
						List<AutoOutput.OutRow> outRows = input.getAutoOutput().finish();
						for (AutoOutput.OutRow outRow : outRows) {
							result.addRow(outRow.getPrimaryId(), outRow.getTypes(), outRow.getData());
						}
					}
				}
			}
			//find the optimal subtypes
			log.info("finding optimal column types");
			log.info("\t{}.{}: {} -> {}",
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

			try (com.esotericsoftware.kryo.io.Output out = new com.esotericsoftware.kryo.io.Output(outFile.writeContent())) {
				result.writeToFile(out);
			}

			try (OutputStream out = outFile.writeHeader()) {
				result.writeHeader(out);
			}
		}

		if (errorCounter.get() > 0 && log.isWarnEnabled()) {
			log.warn("File `{}` contained {} faulty lines of ~{} total.", descriptor.getInputFile().getDescriptionFile(), errorCounter.get(), lineId);
		}

		//if successful move the tmp file to the target location
		FileUtils.moveFile(tmp, descriptor.getInputFile().getPreprocessedFile());
		log.info("PREPROCESSING DONE in {}", descriptor.getInputFile().getDescriptionFile());
	}

	private static void parseRow(int primaryId, PPColumn[] columns, String[] row, Input input, long lineId, Preprocessed result, int inputSource, List<OutputDescription.Output> outputs, GroovyPredicate filter) throws ParsingException {

		if (input.checkAutoOutput()) {
			List<AutoOutput.OutRow> outRows = input.getAutoOutput().createOutput(primaryId, row, columns, inputSource, lineId);
			for (AutoOutput.OutRow outRow : outRows) {
				result.addRow(primaryId, columns, outRow.getData());
			}
		}
		else if (filter == null || filter.filterRow(row)) {
			for (Object[] outRow : applyOutputs(outputs, columns, row, inputSource, lineId)) {
				result.addRow(primaryId, columns, outRow);
			}
		}

	}

	private static Integer parsePrimary(StringParser primaryType, String[] row, long lineId, int source, OutputDescription.Output primaryOutput) throws ParsingException {
		List<Object> primary = primaryOutput.createOutput(primaryType, row, source, lineId);

		// Assert that primary produces single strings
		if (primary.size() != 1 || !(primary.get(0) instanceof Integer)) {
			throw new IllegalStateException("The returned primary was the illegal value " + primary + " in " + Arrays.toString(row));
		}

		return (int) primary.get(0);
	}

	/**
	 * Apply each output for a single row. Returning all resulting values.
	 */
	private static List<Object[]> applyOutputs(List<OutputDescription.Output> outputs, PPColumn[] columns, String[] row, int source, long lineId) throws ParsingException {
		List<Object[]> resultRows = new ArrayList<>();

		for (int c = 0; c < outputs.size(); c++) {

			final OutputDescription.Output out = outputs.get(c);
			final Parser<?> parser = columns[c].getParser();

			final List<Object> result = out.createOutput(parser, row, source, lineId);

			if (result == null) {
				throw new IllegalStateException(out + " returned null result for " + Arrays.toString(row));
			}


			//if the result is a single NULL and we don't want to include such rows
			if (result.size() == 1 && result.get(0) == null) {
				return Collections.emptyList();
			}

			if (resultRows.isEmpty()) {
				for (Object v : result) {
					Object[] newRow = new Object[outputs.size()];
					newRow[c] = v;
					resultRows.add(newRow);
				}
			}
			else if (result.size() == 1) {
				for (Object[] resultRow : resultRows) {
					resultRow[c] = result.get(0);
				}
			}
			else {
				List<Object[]> newResultRows = new ArrayList<>(resultRows.size() * result.size());
				for (Object v : result) {
					for (Object[] resultRow : resultRows) {
						Object[] newResultRow = Arrays.copyOf(resultRow, resultRow.length);
						newResultRow[c] = v;
						newResultRows.add(newResultRow);
					}
				}
				resultRows = newResultRows;
			}

		}
		return resultRows;
	}
}