package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
import org.apache.commons.lang3.StringUtils;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)

public class PostalCodesManager {
	private final List<PostalCodeDistance> data;

	/**
	 * This method loads the postcodes and their distances between each other. The loaded postal codes will be passed to the created {@link PostalCodesManager}
	 *
	 * @param csvFilePath Path of file containing the postal codes data as csv
	 * @return Preloaded  {@link PostalCodesManager}
	 */
	static public PostalCodesManager loadFrom(@NonNull @NotEmpty String csvFilePath, boolean zipped) throws IOException {

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


	/**
	 * This method filters out all postcodes that are within the specified distance radius from the specified reference postcode.
	 */
	public String[] filterAllNeighbours(int plz, double radius) {

		if (radius == 0) {
			return new String[]{StringUtils.leftPad(Integer.toString(plz), 5, '0')};
		}

		return data.stream()
				   .takeWhile(postalCodeDistance -> postalCodeDistance.getDistanceInKm() <= radius)
				   .filter(postalCodeDistance -> postalCodeDistance.getLeft() == plz || postalCodeDistance.getRight() == plz)
				   .map(postalCodeDistance -> {
					   if (postalCodeDistance.getLeft() == plz) {
						   return StringUtils.leftPad(Integer.toString(postalCodeDistance.getRight()), 5, '0');
					   }
					   else {
						   return StringUtils.leftPad(Integer.toString(postalCodeDistance.getLeft()), 5, '0');
					   }
				   })
				   .toArray(String[]::new);


	}
}

