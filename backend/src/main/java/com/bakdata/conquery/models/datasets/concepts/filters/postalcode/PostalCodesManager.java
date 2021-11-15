package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PostalCodesManager {

	@Getter
	@Setter
	static private File dataFile = null;


	@Getter
	final static private HashMap<Set<String>, Double> data = new HashMap<>();

	static private boolean loaded = false;


	/**
	 * This method loads the postcodes and their distances between each other
	 * Then it gives a list of the entries read.
	 *
	 * @return The list is NULL if the file concerned has not been set yet
	 */
	static public List<PostalCodeDistanceEntity> loadData() throws IOException, URISyntaxException {
		if (getDataFile() == null) {
			setDataFile(new File(PostalCodesManager.class.getClassLoader().getResource("com.bakdata.conquery/postalcodes.csv").toURI()));
		}
		final BeanListProcessor<PostalCodeDistanceEntity> rowProcessor = new BeanListProcessor<>(PostalCodeDistanceEntity.class);

		final CsvParserSettings csvParserSettings = new CsvParserSettings();
		csvParserSettings.setDelimiterDetectionEnabled(true);
		csvParserSettings.setHeaderExtractionEnabled(true);
		csvParserSettings.setProcessor(rowProcessor);

		final CsvParser parser = new CsvParser(csvParserSettings);
		parser.parse(new InputStreamReader(new FileInputStream(getDataFile()), StandardCharsets.UTF_8));

		final List<PostalCodeDistanceEntity> codeDistanceEntityList = rowProcessor.getBeans();

		codeDistanceEntityList.forEach(elt -> {
			final Double lastValue = data.putIfAbsent(Set.of(elt.getPlz1(), elt
					.getPlz2()), elt.getDistance());
			if (lastValue != null) {
				log.error("Two found distances found for postal codes {} & {} : {} km (First value) and {} km", elt.getPlz1(), elt.getPlz2(), lastValue, elt.getDistance());
			}
		});
		loaded = true;
		return codeDistanceEntityList;
	}


	/**
	 * This method filters out all postcodes that are within the specified distance radius from the specified reference postcode (reference-postcode included).
	 */
	static public List<String> filterAllNeighbours(PostalCodeSearchEntity postalCodeSearchEntity) {
		try {
			if (postalCodeSearchEntity.getRadius() == 0) {
				return List.of(postalCodeSearchEntity.getPlz());
			}
			if (!loaded) {
				loadData();
			}
			final List<String> foundPostalCodes = getData().entrySet()
														   .stream()
														   .filter(entry -> entry.getKey().contains(postalCodeSearchEntity.getPlz())
																			&& entry.getValue() <= postalCodeSearchEntity.getRadius())
														   .map(Map.Entry::getKey)
														   .flatMap(Collection::stream)
														   .collect(Collectors.toList());

			//remove all occurrences of the reference postal code
			foundPostalCodes.removeIf(elt -> elt.equals(postalCodeSearchEntity.getPlz()));

			//To have at least one element of reference plz , we have to add it again to the result list
			foundPostalCodes.add(postalCodeSearchEntity.getPlz());

			return foundPostalCodes;
		}
		catch (Exception error) {
			log.error("{}", error);
			return Collections.emptyList();
		}

	}
}

