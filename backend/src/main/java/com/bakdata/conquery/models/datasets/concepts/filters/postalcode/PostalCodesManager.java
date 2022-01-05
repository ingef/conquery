package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString()
public class PostalCodesManager implements Injectable {
	private final List<PostalCodeDistance> data;

	/**
	 * This method loads the postcodes and their distances between each other. The loaded postal codes will be passed to the created {@link PostalCodesManager}
	 *
	 * @param csvFilePath Path of file containing the postal codes data as csv
	 * @return Preloaded  {@link PostalCodesManager}
	 */
	@SneakyThrows(IOException.class)
	public static PostalCodesManager loadFrom(@NonNull @NotEmpty String csvFilePath, boolean zipped) {

		final PostalCodeProcessor rowProcessor = new PostalCodeProcessor();
		final CsvParserSettings csvParserSettings = new CsvParserSettings();
		csvParserSettings.setDelimiterDetectionEnabled(true);
		csvParserSettings.setHeaderExtractionEnabled(true);
		csvParserSettings.setProcessor(rowProcessor);

		final InputStream stream =
				zipped
				? new GZIPInputStream(In.resource(csvFilePath).asStream())
				: In.resource(csvFilePath).asStream();

		final CsvParser parser = new CsvParser(csvParserSettings);

		parser.parse(stream);

		return new PostalCodesManager(rowProcessor.getData());

	}


	/**
	 * This method filters out all postcodes that are within the specified distance radius from the specified reference postcode (reference postal code included).
	 */
	public String[] filterAllNeighbours(int plz, double radius) {
		final List<String> foundPLZ = new ArrayList<>();
		foundPLZ.add(padPlz(plz));

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
			.map(PostalCodesManager::padPlz)
			.forEach(foundPLZ::add);

		return foundPLZ.toArray(String[]::new);
	}

	private static String padPlz(Integer other) {
		return StringUtils.leftPad(Integer.toString(other), 5, '0');
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(PostalCodesManager.class, this);
	}
}

