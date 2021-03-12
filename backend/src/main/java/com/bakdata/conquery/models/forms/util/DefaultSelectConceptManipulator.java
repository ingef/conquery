package com.bakdata.conquery.models.forms.util;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Sets {@link Select}s on a {@link CQConcept} that are flaged with default=true depending on the {@link FillMethod}.
 */
@NoArgsConstructor
@AllArgsConstructor
public class DefaultSelectConceptManipulator implements ConceptManipulator {
	public static enum FillMethod {
		OVERWRITE {
			@Override
			public void fill(CQConcept concept) {
				concept.setSelects(concept.getConcept().getDefaultSelects());

				for (CQTable t : concept.getTables()) {
					t.setSelects(t.getConnector().getDefaultSelects());
				}
			}
		},                // Overwrite all present selects with the default value.
		/**
		 * Add default selects to all present selects.
		 */
		ADD {
			@Override
			public void fill(CQConcept concept) {
				List<Select> cSelects = new ArrayList<>(concept.getSelects());
				cSelects.addAll(concept.getConcept().getDefaultSelects());

				concept.setSelects(cSelects);

				for (CQTable t : concept.getTables()) {
					List<Select> conSelects = new ArrayList<>(t.getSelects());
					conSelects.addAll(t.getConnector().getDefaultSelects());
					t.setSelects(conSelects);
				}
			}
		},
		/**
		 * Add default selects only if no selects were present in the whole concept.
		 */
		ADD_TO_COMPLETE_EMPTY{

			@Override
			public void fill(CQConcept concept) {
				boolean allTablesEmpty = concept.getTables().isEmpty()
										 || concept.getTables().stream()
												   .map(CQTable::getSelects)
												   .allMatch(List::isEmpty);

				if(!(concept.getSelects().isEmpty() && allTablesEmpty)) {
					// Don't fill if there are any selects on concept level or on any table level
					return;
				}

				ADD.fill(concept);
			}
		};


		public abstract void fill(CQConcept concept);
	}
		
	private  FillMethod method = FillMethod.ADD_TO_COMPLETE_EMPTY;

	@Override
	public void consume(CQConcept concept, DatasetRegistry namespaces) {
		// Obtain the concept Id
		List<ConceptElement<?>> conceptElements = concept.getElements();
		if(conceptElements.isEmpty()) {
			throw new IllegalArgumentException(String.format("Cannot set defaults on a CQConcept without ids. Provided concept: %s", concept));
		}

		method.fill(concept);
	}

}
