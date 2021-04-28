package com.bakdata.conquery.io.result.excel;

import com.bakdata.conquery.io.result.arrow.ArrowResultGenerationTest;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.vector.util.JsonStringArrayList;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ExcelResultRenderTest {

	public static final ConqueryConfig CONFIG = new ConqueryConfig(){{
		// Surpress java.lang.NoClassDefFoundError: com/bakdata/conquery/io/jackson/serializer/CurrencyUnitDeserializer
		setStorage(new NonPersistentStoreFactory());
	}};
	List<String> printIdFields = List.of("id1", "id2");

	static {
		I18n.init();
	}



	private List<ResultType> getResultTypes() {
		return List.of(
				ResultType.BooleanT.INSTANCE,
				ResultType.IntegerT.INSTANCE,
				ResultType.NumericT.INSTANCE,
				ResultType.CategoricalT.INSTANCE,
				ResultType.ResolutionT.INSTANCE,
				ResultType.DateT.INSTANCE,
				ResultType.DateRangeT.INSTANCE,
				ResultType.StringT.INSTANCE,
				ResultType.MoneyT.INSTANCE,
				new ResultType.ListT(ResultType.BooleanT.INSTANCE)
		);
	}

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
		List<EntityResult> results = List.of(
				new SinglelineEntityResult(1, new Object[]{Boolean.TRUE, 2345634, 123423.34, "CAT1", DateContext.Resolution.DAYS.toString(), 5646, List.of(345, 534), "test_string", 4521, List.of(true, false)}),
				new SinglelineEntityResult(2, new Object[]{Boolean.FALSE, null, null, null, null, null, null, null, null, List.of()}),
				new SinglelineEntityResult(2, new Object[]{Boolean.TRUE, null, null, null, null, null, null, null, null, List.of(false, false)}),
				new MultilineEntityResult(3, List.of(
						new Object[]{Boolean.FALSE, null, null, null, null, null, null, null, null, List.of(false)},
						new Object[]{Boolean.TRUE, null, null, null, null, null, null, null, null, null},
						new Object[]{Boolean.TRUE, null, null, null, null, null, null, null, 4, List.of(true, false, true, false)}
				)));

		ManagedQuery mquery = new ManagedQuery(null, null, null) {
			public List<ResultInfo> getResultInfo() {
				ResultInfoCollector coll = new ResultInfoCollector();
				coll.addAll(getResultTypes().stream()
						.map(ArrowResultGenerationTest.TypedSelectDummy::new)
						.map(select -> new SelectResultInfo(select, new CQConcept()))
						.collect(Collectors.toList()));
				return coll.getInfos();
			}

			;

			public List<EntityResult> getResults() {
				return new ArrayList<>(results);
			}
		};

		// First we write to the buffer, than we read from it and parse it as TSV
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		ExcelRenderer.renderToStream(
				printSettings,
				printIdFields,
				mquery,
				output);

		InputStream inputStream = new ByteArrayInputStream(output.toByteArray());


		XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
		XSSFSheet sheet = workbook.getSheetAt(0);

		List<String> computed = new ArrayList<>();
		int i = 0;
		for (Row row : sheet) {
			StringJoiner sj = new StringJoiner("\t");
			for (Cell cell : row) {
				String format = workbook.createDataFormat().getFormat(cell.getCellStyle().getDataFormat());
				DataFormatter formatter = new DataFormatter();
				switch (cell.getCellType()) {
					case STRING:
					case FORMULA:
					case BOOLEAN:
						sj.add(formatter.formatCellValue(cell));
						break;
					case NUMERIC:
						sj.add(formatter.formatRawCellContents(cell.getNumericCellValue(),cell.getCellStyle().getDataFormat(),format));
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



		List<String> expected = generateExpectedTSV(results, mquery.getResultInfo(), printSettings);

		log.info("Wrote and than read this excel data: \n{}", computed);

		assertThat(computed).isNotEmpty();
		assertThat(computed).isEqualTo(expected);

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
