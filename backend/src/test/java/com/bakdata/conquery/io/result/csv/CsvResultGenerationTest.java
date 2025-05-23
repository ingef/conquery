package com.bakdata.conquery.io.result.csv;

import static com.bakdata.conquery.io.result.ResultTestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
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
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.query.resultinfo.printers.StringResultPrinters;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Slf4j
public class CsvResultGenerationTest {

	public static final ConqueryConfig CONFIG = new ConqueryConfig() {{
		// Suppress java.lang.NoClassDefFoundError: com/bakdata/conquery/io/jackson/serializer/CurrencyUnitDeserializer
		setStorage(new NonPersistentStoreFactory());
	}};

	static {
		I18n.init();
	}

	@ParameterizedTest()
	@ValueSource(strings = {"UTF-8", "WINDOWS-1252"})
	void writeAndRead(String charsetName) throws IOException {
		final Charset charset = Charset.forName(charsetName);

		// Prepare every input data
		final PrintSettings printSettings = new PrintSettings(true,
															  Locale.GERMANY,
															  null,
															  CONFIG,
															  (cer) -> EntityPrintId.from(cer.getEntityId(), cer.getEntityId()),
															  (selectInfo) -> selectInfo.getSelect().getLabel()
		);
		// The Shard nodes send Object[] but since Jackson is used for deserialization, nested collections are always a list because they are not further specialized
		final List<EntityResult> results = getTestEntityResults();

		final ManagedQuery mquery = getTestQuery();

		// First we write to the buffer, than we read from it and parse it as TSV
		final ByteArrayOutputStream bufferOut = new ByteArrayOutputStream();

		try (Writer writer = new BufferedWriter(new OutputStreamWriter(bufferOut, charset))) {

			final CsvRenderer renderer = new CsvRenderer(CONFIG.getCsv().createWriter(writer), printSettings);
			renderer.toCSV(getIdFields(), mquery.getResultInfos(), mquery.streamResults(OptionalLong.empty()), printSettings, charset);
		}

		final String computed = bufferOut.toString(charset);

		final StringResultPrinters printers = StringResultPrinters.forCharset(charset);
		final String expected = generateExpectedCSV(results, mquery.getResultInfos(), printSettings, printers);

		log.info("Wrote and than read this csv data: {}", computed);

		assertThat(computed).isNotEmpty();
		assertThat(computed).isEqualTo(expected);

	}

	private String generateExpectedCSV(List<EntityResult> results, List<ResultInfo> resultInfos, PrintSettings printSettings, PrinterFactory printerFactory) {
		final List<String> expected = new ArrayList<>();
		expected.add(getIdFields().stream().map(info -> info.defaultColumnName(printSettings)).collect(Collectors.joining(","))
					 + ","
					 + getResultTypes().stream()
									   .map(ResultType::typeInfo)
									   .collect(Collectors.joining(","))
					 + "\n");

		final String delimiter = String.valueOf(CONFIG.getCsv().getDelimeter());

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
						   final String printVal = (String) info.createPrinter(printerFactory, printSettings).apply(val);

						   valueJoiner.add(printVal.contains(delimiter) ? "\"" + printVal + "\"" : printVal);
					   }

					   expected.add(valueJoiner + "\n");
				   }
			   });

		return String.join("", expected);
	}
}
