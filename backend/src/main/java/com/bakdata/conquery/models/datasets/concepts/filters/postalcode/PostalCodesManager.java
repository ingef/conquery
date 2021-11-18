package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.github.powerlibraries.io.In;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PostalCodesManager {

	@Getter
	@Setter
	private final String dataFilePath;

	@Getter
	private final List<Double> data;

	private final int recordsNumber = 0;

	public PostalCodesManager() {
		try {
			loadData();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}


	/**
	 * This method loads the postcodes and their distances between each other
	 */
	private void loadData() throws IOException, URISyntaxException {
		if (getDataFilePath() == null) {
			setDataFilePath("/com/bakdata/conquery/postalcodes.csv.gz");
		}


		final PostalCodeProcessor rowProcessor = new PostalCodeProcessor();

		final CsvParserSettings csvParserSettings = new CsvParserSettings();
		csvParserSettings.setDelimiterDetectionEnabled(true);
		csvParserSettings.setHeaderExtractionEnabled(false);
		csvParserSettings.setProcessor(rowProcessor);

		final CsvParser parser = new CsvParser(csvParserSettings);
		parser.parse(new InputStreamReader(new GZIPInputStream(In.resource(getDataFilePath()).asStream()), StandardCharsets.US_ASCII));


		data = rowProcessor.getData();
		recordsNumber = rowProcessor.getRecordsNumber();
	}


	/**
	 * This method filters out all postcodes that are within the specified distance radius from the specified reference postcode (reference-postcode included).
	 */
	public String[] filterAllNeighbours(PostalCodeSearchEntity postalCodeSearchEntity) {
		try {
			final double plz = Double.parseDouble(postalCodeSearchEntity.getPlz());
			if (postalCodeSearchEntity.getRadius() == 0) {
				return new String[]{String.format("%05d", (int) plz)};
			}

			final List<String> foundPostalCodesList = new ArrayList<>();
			for (int i = 0; i < recordsNumber; i++) {
				final double plz1 = data.get(i);
				final double plz2 = data.get(++i);
				final double distance = data.get(++i);

				if (plz1 == plz && distance <= postalCodeSearchEntity.getRadius()) {
					foundPostalCodesList.add(String.format("%05d", (int) plz2));
				}
				else if (plz2 == plz && distance <= postalCodeSearchEntity.getRadius()) {
					foundPostalCodesList.add(String.format("%05d", (int) plz1));
				}
			}
			String[] foundPostalCodes = new String[foundPostalCodesList.size()];
			foundPostalCodesList.toArray(foundPostalCodes);
			return foundPostalCodes;
		}
		catch (Exception error) {
			log.error("{}", error);
			return new String[0];
		}

	}
}

