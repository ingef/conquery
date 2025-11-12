package com.bakdata.conquery.mode.local;

import com.bakdata.conquery.mode.StorageListener;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import lombok.Data;
import com.bakdata.conquery.sql.conquery.SqlMatchingStats;
import com.bakdata.conquery.sql.conversion.dialect.PostgreSqlDialect;

@Data
public class LocalStorageListener implements StorageListener {

	@Override
	public void onAddSecondaryId(SecondaryIdDescription secondaryId) {
	}

	@Override
	public void onDeleteSecondaryId(SecondaryIdDescriptionId description) {
	}

	@Override
	public void onAddTable(Table table) {

	}

	@Override
	public void onRemoveTable(TableId table) {
	}

	@Override
	public void onAddConcept(Concept<?> concept) {
		new SqlMatchingStats().createFunctionForConcept((TreeConcept) concept, new PostgreSqlDialect().getFunctionProvider());
	}

	@Override
	public void onDeleteConcept(ConceptId concept) {
	}
}
