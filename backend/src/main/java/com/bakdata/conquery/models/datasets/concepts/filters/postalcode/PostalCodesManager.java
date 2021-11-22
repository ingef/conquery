package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

import com.github.powerlibraries.io.In;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)

public class PostalCodesManager {
	private final List<PostalCodeRecord> data;

	/**
	 * This method loads the postcodes and their distances between each other. The loaded postal codes will be passed to the created {@link PostalCodesManager}
	 *
	 * @param csvFilePath Path of file containing the postal codes data as csv
	 * @return Preloaded  {@link PostalCodesManager}
	 */
	static public PostalCodesManager newPostalCodesManagerPreloaded(@NonNull @NotEmpty String csvFilePath, boolean zipped) {

		try {

			final PostalCodeProcessor rowProcessor = new PostalCodeProcessor();
			final CsvParserSettings csvParserSettings = new CsvParserSettings();
			csvParserSettings.setDelimiterDetectionEnabled(true);
			csvParserSettings.setHeaderExtractionEnabled(true);
			csvParserSettings.setProcessor(rowProcessor);

			final CsvParser parser = new CsvParser(csvParserSettings);
			if (zipped) {
				parser.parse(new InputStreamReader(new GZIPInputStream(In.resource(csvFilePath).asStream()), StandardCharsets.US_ASCII));
			}
			else {
				parser.parse(new InputStreamReader(In.resource(csvFilePath).asStream(), StandardCharsets.US_ASCII));
			}
			return new PostalCodesManager(rowProcessor.getData());
		}
		catch (IOException exception) {
			log.error("{}", exception);
			return new PostalCodesManager(Collections.emptyList());
		}
	}


	/**
	 * This method filters out all postcodes that are within the specified distance radius from the specified reference postcode (reference-postcode included).
	 */
	public String[] filterAllNeighbours(@Min(1) int plz, @Min(0) double radius) {
		try {
			if (radius == 0) {
				return new String[]{String.format("%05d", plz)};
			}

			final List<String>
					foundPostalCodesList =
					data.stream()
						.filter(postalCodeRecord -> (postalCodeRecord.getPlz1() == plz || postalCodeRecord.getPlz2() == plz)
													&& postalCodeRecord.getDistance_km() <= radius)
						.map(postalCodeRecord -> {
							if (postalCodeRecord.getPlz1() == plz) {
								return String.format("%05d", postalCodeRecord.getPlz2());
							}
							else {
								return String.format("%05d", postalCodeRecord.getPlz1());
							}
						})
						.toArray(String[]::new);

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

