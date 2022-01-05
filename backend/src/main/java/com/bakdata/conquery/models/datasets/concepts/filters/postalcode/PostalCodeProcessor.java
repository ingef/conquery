package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.AbstractRowProcessor;
import it.unimi.dsi.fastutil.ints.IntIntImmutableSortedPair;
import lombok.extern.slf4j.Slf4j;

/**
 * This class processes the abstraction of all postal code data from a csv file
 * The file should have at least the columns <b>plz1</b>, <b>plz2</b> and <b>Distanz_convert_in_km</b>
 * The loaded data will be parsed into a list of {@link PostalCodeDistance} containing unique and distinct pairs of (plz1,plz2)
 */
@Slf4j
public class PostalCodeProcessor extends AbstractRowProcessor {
	private static final String LEFT = "plz1";
	private static final String RIGHT = "plz2";
	private static final String DISTANCE = "Distanz_convert_in_km";

	private int plz1Index, plz2Index, distanceIndex;

	private final Set<IntIntImmutableSortedPair> loadedPlzCombinations = new HashSet<>();

	/**
	 * loaded {@link PostalCodeDistance}-data
	 */
	private final List<PostalCodeDistance> data = new ArrayList<>();

	public List<PostalCodeDistance> getData() {
		data.sort(Comparator.comparingDouble(PostalCodeDistance::getDistanceInKm));
		return data;
	}

	@Override
	public void processStarted(ParsingContext context) {
		super.processStarted(context);
		final String[] headers = context.headers();
		plz1Index =
				IntStream.range(0, headers.length)
						 .filter(i -> LEFT.equals(headers[i]))
						 .findFirst()
						 .orElseThrow(() -> new IllegalStateException("Required Column[plz1] is missing in Headers."));

		plz2Index =
				IntStream.range(0, headers.length)
						 .filter(i -> RIGHT.equals(headers[i]))
						 .findFirst()
						 .orElseThrow(() -> new IllegalStateException("Required Column[plz2] is missing in Headers."));

		distanceIndex =
				IntStream.range(0, headers.length)
						 .filter(i -> DISTANCE.equals(headers[i]))
						 .findFirst()
						 .orElseThrow(() -> new IllegalStateException("Required Column[Distanz_convert_in_km] is missing in Headers."));


		context.skipLines(1); //skip headers when reading rows
	}


	@Override
	public void rowProcessed(String[] row, ParsingContext context) {

		int plz1 = Integer.parseInt(row[plz1Index]);
		int plz2 = Integer.parseInt(row[plz2Index]);
		double distance = Double.parseDouble(row[distanceIndex].trim());

		//checks if the distance of the combination (plz1,plz2) or (plz2,plz1)  has been already added in data list
		if (plz1 != plz2 && loadedPlzCombinations.add(IntIntImmutableSortedPair.of(plz1, plz2))) {
			data.add(new PostalCodeDistance(plz1, plz2, distance));
		}


	}
}
