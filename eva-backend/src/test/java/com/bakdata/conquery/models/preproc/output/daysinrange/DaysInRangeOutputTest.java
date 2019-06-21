package com.bakdata.conquery.models.preproc.output.daysinrange;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.LongStream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.PreprocessingDirectories;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.bakdata.conquery.models.preproc.PPHeader;
import com.bakdata.conquery.models.preproc.Preprocessor;
import com.bakdata.conquery.util.io.ProgressBar;
import com.bakdata.conquery.util.io.SmallIn;
import com.bakdata.eva.models.preproc.output.daysinrange.DaysInRange;
import com.bakdata.eva.models.preproc.output.daysinrange.Identifier;
import com.bakdata.eva.models.preproc.output.daysinrange.PatientEvent;
import com.bakdata.eva.models.preproc.output.daysinrange.TypeIdDaysInRangeMerger;
import com.fasterxml.jackson.core.JsonParser;
import com.github.powerlibraries.io.In;
import com.github.powerlibraries.io.Out;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DaysInRangeOutputTest {

	private Path workDir;
	private Path csvFile;
	private Path importFile;

	@BeforeEach
	public void initializeWorkDir() throws IOException {
		workDir = Files.createTempDirectory("conq.");

		log.info("Working in {}", workDir);

		final Path csv = workDir.resolve("days_in_range.csv");
		final Path importJson = workDir.resolve("days_in_range.import.json");

		csvFile = Files.createFile(csv);
		importFile = Files.createFile(importJson);

		Out.file(csv.toFile()).copyFrom(In.resource(DaysInRangeOutputTest.class, "days_in_range.csv").asStream());
		Out.file(importJson.toFile())
				.copyFrom(In.resource(DaysInRangeOutputTest.class, "days_in_range.import.json").asStream());
	}

	@AfterEach
	public void deleteWorkDir() {
		csvFile.toFile().delete();
		importFile.toFile().delete();
		workDir.toFile().delete();
	}

	@Test
	public void ensureFileMerging() {
		CsvParserSettings csvParserSettings = new CsvParserSettings();
		csvParserSettings.setHeaderExtractionEnabled(true);
		CsvParser csvParser = new CsvParser(csvParserSettings);

		ArrayList<PatientEvent> output = new ArrayList<>();

		TypeIdDaysInRangeMerger merger = TypeIdDaysInRangeMerger.builder().consumer(output::add)
				.mergeCondition(daysInRange -> QuarterUtils.isBeginOfQuarter(daysInRange.getStart())
						|| QuarterUtils.isEndOfQuarter(daysInRange.getEnd()))
				.rangeFunction(DaysInRange::rangeFromStart).build();

		for (Record record : csvParser.iterateRecords(csvFile.toFile())) {
			// unique,year,quarter,days

			Integer year = record.getInt("year");
			Integer quarter = record.getInt("quarter");

			Integer days = record.getInt("days");
			String unique = record.getString("unique");

			CDateRange range = QuarterUtils.fromQuarter(year, quarter);

			DaysInRange daysInRange = new DaysInRange(range, days);
			TypeIdDaysInRangeMerger.DaysInRangeEntry entry = new TypeIdDaysInRangeMerger.DaysInRangeEntry(0,
					new PPColumn[0], daysInRange, new Identifier(List.of(1)), unique);

			merger.process(entry);
		}

		merger.clearRemaining();

		assertThat(output).hasSize(4);
	}

	@Test
	public void test() throws IOException, JSONException, ParsingException {
		ConqueryConfig config = new ConqueryConfig();

		PreprocessingDirectories preprocessingDirectories = new PreprocessingDirectories();

		preprocessingDirectories.setCsv(workDir.toFile());
		preprocessingDirectories.setDescriptions(workDir.toFile());
		preprocessingDirectories.setPreprocessedOutput(workDir.toFile());
		config.getPreprocessor().setDirectories(new PreprocessingDirectories[] {preprocessingDirectories});

		Collection<Preprocessor> descriptors = PreprocessorCommand.findPreprocessingJobs(config, Validators.newValidator());

		ProgressBar progress = new ProgressBar(
			descriptors.stream().mapToLong(Preprocessor::getTotalCsvSize).sum(),
			System.out
		);
		for (Preprocessor descriptor : descriptors) {
			descriptor.preprocess(progress);
		}

		parse(workDir.resolve("days_in_range.cqpp").toFile());
	}

	private void parse(File importFile) throws IOException {
		try (HCFile file = new HCFile(importFile, false)) {
			log.info("Reading HCFile {}:\n\theader size: {}\n\tcontent size: {}", importFile,
					FileUtils.byteCountToDisplaySize(file.getHeaderSize()),
					FileUtils.byteCountToDisplaySize(file.getContentSize()));

			PPHeader header;

			try (JsonParser p = Jackson.BINARY_MAPPER.getFactory().createParser(file.readHeader())) {
				header = Jackson.BINARY_MAPPER.readValue(p, PPHeader.class);

				PPColumn[] columns = header.getColumns();

				log.info("Importing {}", header.getName());

				log.debug("parsing dictionaries");
				header.getPrimaryColumn().getType().readHeader(p);

				log.info("Primary Column: {}", header.getPrimaryColumn().getName());

				for (PPColumn col : columns) {
					col.getType().readHeader(p);
					log.info("colname: {} : {}", col.getName(), col.getType());
				}
			}
			try (SmallIn in = new SmallIn(file.readContent())) {
				PPColumn[] columns = header.getColumns();
				Import imp = Import.createForPreprocessing("table", "tag", columns);

				Bucket[] groups = LongStream.range(0, header.getGroups()).mapToObj(group -> readBlock(imp, in, columns))
						.toArray(Bucket[]::new);

				assertThat(groups).hasSize(3);
				assertThat(groups[0].getNumberOfEvents()).isEqualTo(1);
				assertThat(groups[1].getNumberOfEvents()).isEqualTo(1);
				assertThat(groups[2].getNumberOfEvents()).isEqualTo(2);

				{
					Column column0 = new Column();
					column0.setPosition(0);

					assertThat(groups[2].getDateRange(0, column0)).isInstanceOf(CDateRange.class)
						.isEqualTo(CDateRange.of(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 4, 1)));

					assertThat(groups[0].getDateRange(0, column0)).isInstanceOf(CDateRange.class)
						// First quarter and 1 day, left aligned
						.isEqualTo(CDateRange.of(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 31)));

					assertThat(groups[2].getDateRange(1, column0)).isInstanceOf(CDateRange.class)
						// Fourth quarter and 1 day, right aligned
						.isEqualTo(CDateRange.of(LocalDate.of(2018, 9, 30), LocalDate.of(2018, 12, 31)));
				}

				{
					Column column0 = new Column();
					column0.setPosition(4);

					assertThat(groups[0].getInteger(0, column0))
						.isEqualTo(1);

					assertThat(groups[1].getInteger(0, column0))
						.isEqualTo(2);

					assertThat(groups[2].getInteger(0, column0))
						.isEqualTo(3);

				}
			}
		}
	}

	private static Bucket readBlock(Import imp, SmallIn input, PPColumn[] columns) {
		try {
			// one bit for every column(non-primary) plus one for constant size
			byte[] nullBytes = new byte[(columns.length + 7) >> 3];
			// entityId
			int entity = input.readInt(true);
			int size = input.readInt(true);
	
			byte[] bytes = input.readBytes(size);
	
			Bucket block = imp.getBlockFactory().readSingleValue(0, imp, new ByteArrayInputStream(bytes));
			return block;
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

}
