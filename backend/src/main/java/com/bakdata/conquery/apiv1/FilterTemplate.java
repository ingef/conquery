package com.bakdata.conquery.apiv1;

import java.io.File;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.univocity.parsers.csv.CsvParser;
import io.dropwizard.util.Strings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;

@Data
@AllArgsConstructor
@NoArgsConstructor(onConstructor_ = {@JsonCreator})
@EqualsAndHashCode
@JsonIgnoreProperties({"columns"})
@ToString
@Slf4j
public class FilterTemplate implements Searchable {

	private static final long serialVersionUID = 1L;

	/**
	 * Path to CSV File.
	 */
	private String filePath;

	/**
	 * Value to be sent for filtering.
	 */
	private String columnValue;
	/**
	 * Value displayed in Select list. Usually concise display.
	 */
	private String value;
	/**
	 * More detailed value. Displayed when value is selected.
	 */
	private String optionValue;

	private int minSuffixLength = 3;
	private boolean generateSuffixes = true;


	@Override
	public Stream<FEValue> getSearchValues(CSVConfig config, NamespaceStorage storage) {
		return fromTemplate(this, config);
	}

	private static Stream<FEValue> fromTemplate(FilterTemplate template, CSVConfig parserConfig) {
		final CsvParser parser = parserConfig.createParser();

		// It is likely that multiple Filters reference the same file+config. However we want to ensure it is read only once to avoid wasting computation.
		// We use Streams below to ensure a completely transparent lazy execution of parsing reference files
		return Stream.of(new File(template.getFilePath()))
					 .map(parser::iterateRecords)
					 // Univocity parser does not support streams, so we create one manually using their spliterator.
					 .flatMap(iter -> StreamSupport.stream(iter.spliterator(), false))
					 .map(row -> {
						 // StringSubstitutor will coalesce null values to the raw template string which isn't nice looking.
						 final StringSubstitutor substitutor =
								 new StringSubstitutor(key -> Strings.nullToEmpty(row.getString(key)), "{{", "}}", StringSubstitutor.DEFAULT_ESCAPE);

						 final String rowId = row.getString(template.getColumnValue());

						 if (rowId == null) {
							 log.warn("No id for row `{}` for `{}`", row, template);
							 return null;
						 }

						 final String label = substitutor.replace(template.getValue());
						 final String optionValue = substitutor.replace(template.getOptionValue());

						 return new FEValue(rowId, label, optionValue);
					 })
					 .filter(Objects::nonNull)
					 .distinct();
	}
}
