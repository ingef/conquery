package com.bakdata.conquery.io.result.csv;

import static com.bakdata.conquery.io.result.ResultTestUtil.*;
import static com.bakdata.conquery.io.result.ResultTestUtil.getTestEntityResults;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.io.result.ResultTestUtil;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

@Slf4j
public class CsvResultGenerationTest {

	static {
		I18n.init();
	}

	public static final ConqueryConfig CONFIG = new ConqueryConfig(){{
		// Suppress java.lang.NoClassDefFoundError: com/bakdata/conquery/io/jackson/serializer/CurrencyUnitDeserializer
		setStorage(new NonPersistentStoreFactory());
	}};


	@Test
	void writeAndRead() throws IOException {
		// Prepare every input data
		PrintSettings printSettings = new PrintSettings(
				true,
				Locale.GERMAN,
				null,
				CONFIG,
				(cer) -> EntityPrintId.from(Integer.toString(cer.getEntityId()), Integer.toString(cer.getEntityId())),
				(selectInfo) -> selectInfo.getSelect().getLabel());
		// The Shard nodes send Object[] but since Jackson is used for deserialization, nested collections are always a list because they are not further specialized
		List<EntityResult> results = getTestEntityResults();

		ManagedQuery mquery = getTestQuery();

		// First we write to the buffer, than we read from it and parse it as TSV
		StringWriter writer = new StringWriter();

		CsvRenderer renderer = new CsvRenderer(CONFIG.getCsv().createWriter(writer), printSettings);
		renderer.toCSV(ResultTestUtil.ID_FIELDS, mquery.getResultInfos(), mquery.streamResults());

		String computed = writer.toString();


		String expected = generateExpectedCSV(results, mquery.getResultInfos(), printSettings);

		log.info("Wrote and than read this csv data: {}", computed);

		assertThat(computed).isNotEmpty();
		assertThat(computed).isEqualTo(expected);

	}

	private String generateExpectedCSV(List<EntityResult> results, List<ResultInfo> resultInfos, PrintSettings settings) {
		List<String> expected = new ArrayList<>();
		expected.add(ResultTestUtil.ID_FIELDS.stream().map(info -> info.defaultColumnName(settings)).collect(Collectors.joining(",")) + "," + getResultTypes().stream().map(ResultType::typeInfo).collect(Collectors.joining(",")) + "\n");
		results.stream()
				.map(EntityResult.class::cast)
				.forEach(res -> {

					for (Object[] line : res.listResultLines()) {
						StringJoiner valueJoiner = new StringJoiner(",");
						valueJoiner.add(String.valueOf(res.getEntityId()));
						valueJoiner.add(String.valueOf(res.getEntityId()));
						for (int lIdx = 0; lIdx < line.length; lIdx++) {
							Object val = line[lIdx];
							if(val == null) {
								valueJoiner.add("");
								continue;
							}
							ResultInfo info = resultInfos.get(lIdx);
							final String printVal = info.getType().printNullable(settings, val);
							valueJoiner.add(printVal.contains(String.valueOf(CONFIG.getCsv().getDelimeter()))? "\""+printVal+"\"": printVal);
						}

						expected.add(valueJoiner + "\n");
					}
				});

		return String.join("", expected);
	}
}
