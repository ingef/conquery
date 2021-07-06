package com.bakdata.conquery.io.result.csv;

import static com.bakdata.conquery.io.result.ResultTestUtil.getResultTypes;
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
		StringWriter writer = new StringWriter();

		CsvRenderer renderer = new CsvRenderer(CONFIG.getCsv().createWriter(writer), printSettings);
		renderer.toCSV(printIdFields, mquery.getResultInfo(), mquery.streamResults());

		String computed = writer.toString();


		String expected = generateExpectedCSV(results, mquery.getResultInfo(), printSettings);

		log.info("Wrote and than read this csv data: {}", computed);

		assertThat(computed).isNotEmpty();
		assertThat(computed).isEqualTo(expected);

	}

	private String generateExpectedCSV(List<EntityResult> results, List<ResultInfo> resultInfos, PrintSettings settings) {
		List<String> expected = new ArrayList<>();
		expected.add(String.join(",", printIdFields) + "," + getResultTypes().stream().map(ResultType::typeInfo).collect(Collectors.joining(",")) + "\n");
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

		return expected.stream().collect(Collectors.joining());
	}
}
