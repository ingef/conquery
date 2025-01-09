package com.bakdata.conquery.io.result.excel;

import static com.bakdata.conquery.io.result.ResultTestUtil.getResultTypes;
import static com.bakdata.conquery.io.result.ResultTestUtil.getTestEntityResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.result.ResultTestUtil;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.ExcelConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.query.resultinfo.printers.StringResultPrinters;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

@Slf4j
public class ExcelResultRenderTest {

	public static final ConqueryConfig CONFIG = new ConqueryConfig(){{
		// Suppress java.lang.NoClassDefFoundError: com/bakdata/conquery/io/jackson/serializer/CurrencyUnitDeserializer
		setStorage(new NonPersistentStoreFactory());
	}};
	private static final List<String> printIdFields = List.of("id1", "id2");

	static {
		I18n.init();
	}

	@Test
	void writeAndRead() throws IOException {
		// Prepare every input data
		final PrintSettings printSettings = new PrintSettings(true,
															  Locale.GERMAN,
															  null,
															  CONFIG,
															  (cer) -> EntityPrintId.from(cer.getEntityId(), cer.getEntityId()),
															  (selectInfo) -> selectInfo.getSelect().getLabel()
		);
		// The Shard nodes send Object[] but since Jackson is used for deserialization, nested collections are always a list because they are not further specialized
		final List<EntityResult> results = getTestEntityResults();

		MetaStorage metaStorage = new MetaStorage(new NonPersistentStoreFactory());
		metaStorage.openStores(null, new MetricRegistry());

		ManagedQuery mquery = getManagedQuery(metaStorage, results);

		// First we write to the buffer, than we read from it and parse it as TSV
		final ByteArrayOutputStream output = new ByteArrayOutputStream();

		final ExcelRenderer renderer = new ExcelRenderer(new ExcelConfig(), printSettings);

		renderer.renderToStream(ResultTestUtil.getIdFields(), mquery, output, OptionalLong.empty(), printSettings, metaStorage);

		final InputStream inputStream = new ByteArrayInputStream(output.toByteArray());


		final List<String> computed = readComputed(inputStream, printSettings);

		// We have to do some magic here to emulate the excel printed results.
		PrintSettings tsvPrintSettings = new PrintSettings(true,
														   Locale.GERMAN,
														   null,
														   CONFIG,
														   (cer) -> EntityPrintId.from(cer.getEntityId(), cer.getEntityId()),
														   (selectInfo) -> selectInfo.getSelect().getLabel()
		);

		final List<String> expected = generateExpectedTSV(results, mquery.getResultInfos(), tsvPrintSettings, new StringResultPrinters());

		log.info("Wrote and than read this excel data: {}", computed);

		assertThat(computed).isNotEmpty();
		assertThat(computed).isEqualTo(expected);

	}

	private static @NotNull ManagedQuery getManagedQuery(MetaStorage metaStorage, List<EntityResult> results) {
		User user = new User("test", "test", metaStorage);
		user.updateStorage();

		return new ManagedQuery(mock(Query.class), user.getId(), new Dataset(ExcelResultRenderTest.class.getSimpleName()).getId(), metaStorage, null) {
			@Override
			public Stream<EntityResult> streamResults(OptionalLong maybeLimit) {
				return results.stream();
			}

			@Override
			public List<ResultInfo> getResultInfos() {
				return getResultTypes().stream()
									   .map(ResultTestUtil.TypedSelectDummy::new)
									   .map(select -> new SelectResultInfo(select, new CQConcept(), Collections.emptySet()))
									   .collect(Collectors.toList());
			}
		};
	}

	@NotNull
	private List<String> readComputed(InputStream inputStream, PrintSettings settings) throws IOException {
		final XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
		final XSSFSheet sheet = workbook.getSheetAt(0);

		final List<String> computed = new ArrayList<>();
		for (Row row : sheet) {
			final StringJoiner sj = new StringJoiner("\t");
			final DataFormatter formatter = new DataFormatter(settings.getLocale());
			for (Cell cell : row) {

				final String formatted = switch (cell.getCellType()) {
					case STRING, FORMULA, BOOLEAN, NUMERIC -> formatter.formatCellValue(cell);
					// We write 'null' here to express that the cell was empty
					case BLANK -> "null";
					default -> throw new IllegalStateException("Unknown cell type: " + cell.getCellType());
				};

				sj.add(formatted);
			}
			computed.add(sj.toString());
		}
		return computed;
	}


	private List<String> generateExpectedTSV(
			List<EntityResult> results, List<ResultInfo> resultInfos, PrintSettings printSettings, PrinterFactory printerFactory) {
		final List<String> expected = new ArrayList<>();
		expected.add(String.join("\t", printIdFields) + "\t" + getResultTypes().stream().map(ResultType::typeInfo).collect(Collectors.joining("\t")));
		results.stream()
			   .map(EntityResult.class::cast)
			   .forEach(res -> {

				   for (Object[] line : res.listResultLines()) {
					   final StringJoiner valueJoiner = new StringJoiner("\t");

					   valueJoiner.add(String.valueOf(res.getEntityId()));
					   valueJoiner.add(String.valueOf(res.getEntityId()));

					   for (int lIdx = 0; lIdx < line.length; lIdx++) {
						   final Object val = line[lIdx];

						   final ResultInfo info = resultInfos.get(lIdx);
						   final String printed = printValue(val, info, printSettings, printerFactory);

						   valueJoiner.add(printed);
					   }
					   expected.add(valueJoiner.toString());
				   }
			   });

		return expected;
	}

	private String printValue(Object val, ResultInfo info, PrintSettings printSettings, PrinterFactory printerFactory) {
		if (val == null) {
			return "null";
		}

		final Printer printer = info.createPrinter(printerFactory, printSettings);

		if (info.getType().equals(ResultType.Primitive.BOOLEAN)) {
			// Even though we set the locale to GERMAN, poi's {@link DataFormatter#formatCellValue(Cell)} hardcoded english booleans
			return (Boolean) val ? "TRUE" : "FALSE";
		}

		return Objects.toString(printer.apply(val));
	}

}
