package com.bakdata.conquery.integration.common;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.DistinctSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.LastValueSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;


/**
 * Helper class to automatically create concepts for test purposes.
 * <p>
 * The concept is derived from a {@link Table} and uses its name and {@link AutoConceptUtil#CONCEPT_NAME_SUFFIX}
 * to construct an id by which it can be referenced.
 */
@UtilityClass
public class AutoConceptUtil {

	public static final String CONCEPT_NAME_SUFFIX = "_AUTO";
	public static final String CONNECTOR_NAME = "connector";

	/**
	 * Generate a {@link TreeConcept} from the provided {@link Table}.
	 * <p>
	 * The concept will have one {@link ConceptTreeConnector} called {@link AutoConceptUtil#CONNECTOR_NAME}.
	 * <p>
	 * For each column in the table, a basic set of {@link Select}s is generated (compare {@link AutoConceptUtil#getAutoSelectsForColumn(Column)}).
	 * The name of a select derives from the {@code [name]} of the column and the {@link CPSType} {@code [type]} of the {@link Select}: {@code [name]_[type]}
	 */
	@NotNull
	public static TreeConcept createConcept(Table table) {

		// Prepare concept
		final TreeConcept concept = new TreeConcept();
		concept.setName(table.getName() + CONCEPT_NAME_SUFFIX);

		// Prepare connnector
		final ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setConcept(concept);
		connector.setName(CONNECTOR_NAME);
		connector.setTable(table);

		// Prepare selects
		List<Select> selects = new ArrayList<>();
		for (Column column : table.getColumns()) {
			selects.addAll(getAutoSelectsForColumn(column));
		}

		// Wire things up
		selects.forEach(select -> select.setHolder(connector));
		connector.setSelects(selects);
		concept.setConnectors(List.of(connector));

		return concept;
	}

	/**
	 * Create a basic set of {@link com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect}s for the provided column.
	 * <p>
	 * Currently, theses selects are generated:
	 * <ul>
	 *     <li>{@link FirstValueSelect}</li>
	 *     <li>{@link LastValueSelect}</li>
	 *     <li>{@link DistinctSelect}</li>
	 * </ul>
	 * <p>
	 * First- and LastValueSelect can be easily used to achieve type similarity between imported data and result data in a test scenario.
	 * DistinctSelect provide a convenient way to check if filters on event basis work properly.
	 *
	 * @param column The column for which the selects are created.
	 * @return The created list of selects.
	 */
	private static List<Select> getAutoSelectsForColumn(Column column) {

		// Build the name prefix for all selects
		final String prefix = column.getName() + "_";

		// Create basic single column selects
		final LastValueSelect last = new LastValueSelect(column, null);
		last.setName(prefix + LastValueSelect.class.getAnnotation(CPSType.class).id());
		last.setColumn(column);

		final FirstValueSelect first = new FirstValueSelect(column, null);
		first.setName(prefix + FirstValueSelect.class.getAnnotation(CPSType.class).id());
		first.setColumn(column);

		final DistinctSelect distinct = new DistinctSelect(column, null);
		distinct.setName(prefix + DistinctSelect.class.getAnnotation(CPSType.class).id());
		distinct.setColumn(column);

		return List.of(
				last,
				first,
				distinct
		);
	}
}
