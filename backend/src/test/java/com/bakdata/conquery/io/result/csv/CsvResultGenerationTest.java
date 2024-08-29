package com.bakdata.conquery.io.result.csv;

import static com.bakdata.conquery.io.result.ResultTestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.printers.StringResultPrinters;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class CsvResultGenerationTest {

	public static final ConqueryConfig CONFIG = new ConqueryConfig() {{
		// Suppress java.lang.NoClassDefFoundError: com/bakdata/conquery/io/jackson/serializer/CurrencyUnitDeserializer
		setStorage(new NonPersistentStoreFactory());
	}};

	static {
		I18n.init();
	}

	@Test
	void writeAndRead() throws IOException {
		// Prepare every input data
		final PrintSettings printSettings = new PrintSettings(
				true,
				Locale.GERMAN,
				null,
				CONFIG,
				(cer) -> EntityPrintId.from(cer.getEntityId(), cer.getEntityId()),
				(selectInfo) -> selectInfo.getSelect().getLabel(), new StringResultPrinters()
		);
		// The Shard nodes send Object[] but since Jackson is used for deserialization, nested collections are always a list because they are not further specialized
		final List<EntityResult> results = getTestEntityResults();

		final ManagedQuery mquery = getTestQuery();

		// First we write to the buffer, than we read from it and parse it as TSV
		final StringWriter writer = new StringWriter();

		final CsvRenderer renderer = new CsvRenderer(CONFIG.getCsv().createWriter(writer), printSettings);
		renderer.toCSV(getIdFields(), mquery.getResultInfos(), mquery.streamResults(OptionalLong.empty()), printSettings);

		final String computed = writer.toString();


		final String expected = generateExpectedCSV(results, mquery.getResultInfos(), printSettings);

		log.info("Wrote and than read this csv data: {}", computed);

		assertThat(computed).isNotEmpty();
		assertThat(computed).isEqualTo(expected);

	}

	private String generateExpectedCSV(List<EntityResult> results, List<ResultInfo> resultInfos, PrintSettings printSettings) {
		final List<String> expected = new ArrayList<>();
		expected.add(getIdFields().stream().map(info -> info.defaultColumnName(printSettings)).collect(Collectors.joining(","))
					 + ","
					 + getResultTypes().stream().map(ResultType::typeInfo).collect(Collectors.joining(","))
					 + "\n");
		results.stream()
			   .map(EntityResult.class::cast)
			   .forEach(res -> {

				   for (Object[] line : res.listResultLines()) {
					   final StringJoiner valueJoiner = new StringJoiner(",");
					   valueJoiner.add(String.valueOf(res.getEntityId()));
					   valueJoiner.add(String.valueOf(res.getEntityId()));
					   for (int lIdx = 0; lIdx < line.length; lIdx++) {
						   final Object val = line[lIdx];
						   if (val == null) {
							   valueJoiner.add("");
							   continue;
						   }
						   final ResultInfo info = resultInfos.get(lIdx);
						   final String printVal = (String) info.createPrinter(printSettings).apply(val);
						   valueJoiner.add(printVal.contains(String.valueOf(CONFIG.getCsv().getDelimeter())) ? "\"" + printVal + "\"" : printVal);
					   }

					   expected.add(valueJoiner + "\n");
				   }
			   });

		return String.join("", expected);
	}
}
