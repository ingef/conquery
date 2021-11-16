package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
	private String dataFilePath = null;

	@Getter
	private List<Double> data;

	private int recordsNumber = 0;

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
			setDataFilePath("/com.bakdata.conquery/postalcodes.csv");
		}


		final PostalCodeProcessor rowProcessor = new PostalCodeProcessor();

		final CsvParserSettings csvParserSettings = new CsvParserSettings();
		csvParserSettings.setDelimiterDetectionEnabled(true);
		csvParserSettings.setHeaderExtractionEnabled(false);
		csvParserSettings.setProcessor(rowProcessor);

		final CsvParser parser = new CsvParser(csvParserSettings);
		parser.parse(new InputStreamReader(In.resource(getDataFilePath()).asStream(), StandardCharsets.UTF_8));


		data = rowProcessor.getData();
		recordsNumber = rowProcessor.getRecordsNumber();
	}


	/**
	 * This method filters out all postcodes that are within the specified distance radius from the specified reference postcode (reference-postcode included).
	 */
	public String[] filterAllNeighbours(PostalCodeSearchEntity postalCodeSearchEntity) {
		try {
			if (postalCodeSearchEntity.getRadius() == 0) {
				return new String[]{String.format("%05d", (int) postalCodeSearchEntity.getPlz())};
			}

			final List<String> foundPostalCodesList = new ArrayList<>();
			for (int i = 0; i < recordsNumber; i++) {
				final double plz1 = data.get(i);
				final double plz2 = data.get(++i);
				final double distance = data.get(++i);

				if (plz1 == postalCodeSearchEntity.getPlz() && distance <= postalCodeSearchEntity.getRadius()) {
					foundPostalCodesList.add(String.format("%05d", (int) plz2));
				}
				else if (plz2 == postalCodeSearchEntity.getPlz() && distance <= postalCodeSearchEntity.getRadius()) {
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

