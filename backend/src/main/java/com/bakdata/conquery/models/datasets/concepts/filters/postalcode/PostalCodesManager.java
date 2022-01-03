package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.github.powerlibraries.io.In;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)

public class PostalCodesManager implements Injectable {
	private final List<PostalCodeDistance> data;

	/**
	 * This method loads the postcodes and their distances between each other. The loaded postal codes will be passed to the created {@link PostalCodesManager}
	 *
	 * @param csvFilePath Path of file containing the postal codes data as csv
	 * @return Preloaded  {@link PostalCodesManager}
	 */
	@SneakyThrows(IOException.class)
	static public PostalCodesManager loadFrom(@NonNull @NotEmpty String csvFilePath, boolean zipped) {

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
	 * This method filters out all postcodes that are within the specified distance radius from the specified reference postcode (reference postal code included).
	 */
	public String[] filterAllNeighbours(int plz, double radius) {
		final List<String> foundPLZ = new ArrayList<>();
		foundPLZ.add(StringUtils.leftPad(Integer.toString(plz), 5, '0'));
			data.stream()
								// This works because data is already sorted
								.takeWhile(postalCodeDistance -> postalCodeDistance.getDistanceInKm() <= radius)
								.filter(postalCodeDistance -> postalCodeDistance.getLeft() == plz || postalCodeDistance.getRight() == plz)
								.map(postalCodeDistance -> {
									if (postalCodeDistance.getLeft() == plz) {
										return postalCodeDistance.getRight();
									}
									else {
										return postalCodeDistance.getLeft();
									}
								})
								.map(other -> StringUtils.leftPad(Integer.toString(other), 5, '0'))
								.forEach(foundPLZ::add);
		return foundPLZ.toArray(String[]::new);
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(PostalCodesManager.class, this);
	}
}

