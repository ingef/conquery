package com.bakdata.conquery.io.result.excel;

import static com.bakdata.conquery.io.result.ResultTestUtil.getResultTypes;
import static com.bakdata.conquery.io.result.ResultTestUtil.getTestEntityResults;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.io.result.ResultTestUtil;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.ExcelConfig;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
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

	static {
		I18n.init();
	}

	public static final ConqueryConfig CONFIG = new ConqueryConfig(){{
		// Suppress java.lang.NoClassDefFoundError: com/bakdata/conquery/io/jackson/serializer/CurrencyUnitDeserializer
		setStorage(new NonPersistentStoreFactory());
	}};
	List<String> printIdFields = List.of("id1", "id2");


	@Test
	void writeAndRead() throws IOException {
		// Prepare every input data
		PrintSettings printSettings = new PrintSettings(
				true,
				Locale.GERMAN,
				null,
				CONFIG,
				(cer) -> new ExternalEntityId(new String[]{Integer.toString(cer.getEntityId()), Integer.toString(cer.getEntityId())}),
				(selectInfo) -> selectInfo.getSelect().getLabel());
		// The Shard nodes send Object[] but since Jackson is used for deserialization, nested collections are always a list because they are not further specialized
		List<EntityResult> results = getTestEntityResults();

		ManagedQuery mquery = new ManagedQuery(null, null, null) {
			public List<ResultInfo> getResultInfo() {
				ResultInfoCollector coll = new ResultInfoCollector();
				coll.addAll(getResultTypes().stream()
						.map(ResultTestUtil.TypedSelectDummy::new)
						.map(select -> new SelectResultInfo(select, new CQConcept()))
						.collect(Collectors.toList()));
				return coll.getInfos();
			}

			;

			@Override
			public Stream<EntityResult> streamResults() {
				return results.stream();
			}
		};

		// First we write to the buffer, than we read from it and parse it as TSV
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		ExcelRenderer renderer = new ExcelRenderer(new ExcelConfig());

		renderer.renderToStream(
				printSettings,
				printIdFields,
				mquery,
				output);

		InputStream inputStream = new ByteArrayInputStream(output.toByteArray());


		List<String> computed = readComputed(inputStream, printSettings);


		List<String> expected = generateExpectedTSV(results, mquery.getResultInfo(), printSettings);

		log.info("Wrote and than read this excel data: {}", computed);

		assertThat(computed).isNotEmpty();
		assertThat(computed).isEqualTo(expected);

	}

	@NotNull
	private List<String> readComputed(InputStream inputStream, PrintSettings settings) throws IOException {
		XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
		XSSFSheet sheet = workbook.getSheetAt(0);

		List<String> computed = new ArrayList<>();
		int i = 0;
		for (Row row : sheet) {
			StringJoiner sj = new StringJoiner("\t");
			for (Cell cell : row) {
				DataFormatter formatter = new DataFormatter(settings.getLocale());
				switch (cell.getCellType()) {
					case STRING:
					case FORMULA:
					case BOOLEAN:
					case NUMERIC:
						sj.add(formatter.formatCellValue(cell));
						break;
					case BLANK:
						// We write 'null' here to express that the cell was empty
						sj.add("null");
						break;
					default: throw new IllegalStateException("Unknown cell type: " + cell.getCellType());
				}
			}
			computed.add(sj.toString());
			i++;
		}
		return computed;
	}


	private List<String> generateExpectedTSV(List<EntityResult> results, List<ResultInfo> resultInfos, PrintSettings settings) {
		List<String> expected = new ArrayList<>();
		expected.add(String.join("\t", printIdFields) + "\t" + getResultTypes().stream().map(ResultType::typeInfo).collect(Collectors.joining("\t")));
		results.stream()
				.map(EntityResult.class::cast)
				.forEach(res -> {

					for (Object[] line : res.listResultLines()) {
						StringJoiner valueJoiner = new StringJoiner("\t");
						valueJoiner.add(String.valueOf(res.getEntityId()));
						valueJoiner.add(String.valueOf(res.getEntityId()));
						for (int lIdx = 0; lIdx < line.length; lIdx++) {
							Object val = line[lIdx];
							if(val == null) {
								valueJoiner.add("null");
								continue;
							}
							ResultInfo info = resultInfos.get(lIdx);
							joinValue(settings, valueJoiner, val, info);
						}
						expected.add(valueJoiner.toString());
					}
				});

		return expected;
	}

	private void joinValue(PrintSettings settings, StringJoiner valueJoiner, Object val, ResultInfo info) {
		String printVal = info.getType().printNullable(settings, val);
		if (info.getType().equals(ResultType.MoneyT.INSTANCE)) {
			printVal = printVal +" €";
		}
		valueJoiner.add(printVal);
	}

}
