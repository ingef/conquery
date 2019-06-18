package com.bakdata.eva.forms.description;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.common.ColumnDescriptor;
import com.bakdata.eva.forms.common.ColumnDescriptor.ColumnType;
import com.bakdata.eva.forms.common.FixedColumn;
import com.bakdata.eva.forms.common.StatisticForm;
import com.bakdata.eva.models.forms.DateContextMode;
import com.bakdata.eva.models.forms.FeatureGroup;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class DescriptionFormBase extends StatisticForm {

	@NotNull
	protected DateContextMode resolution;
	@NotNull
	@NotEmpty
	protected List<CQOr> features;
	@JsonIgnore
	protected User formUser;
	@JsonIgnore
	// Using a list here to have a fixed order for the test
	protected static final List<String> FIXED_COLUMN_NAMES = List.of(
		"alter",
		"geschlecht",
		"versichertentage"
		);

	
	protected static class ColumnManipulator implements BiConsumer<ColumnDescriptor, CQOr> {
		/* Checklist for columns that have already been set as fixed.
		 * If a additional Concept is used, that is already part of 'fixedColumnNames',
		 * this structure is checked and than the column for that Concept is set as ColumnType.VARIABLE.
		 * It is assumed that the fixed columns are processed in the beginning.
		 */
		private Set<String> setFixed = new HashSet<>();
		
		@Override
		public void accept(ColumnDescriptor cd, CQOr or) {

			String name = ((CQConcept)or.getChildren().get(0)).getIds().get(0).getName();
			if ((!setFixed.contains(name)) && FIXED_COLUMN_NAMES.contains(name) ){
				cd.setColumnType(ColumnType.FIXED);
				setFixed.add(name);
			}
			else {
				cd.setColumnType(ColumnType.VARIABLE);
			}
		}
		
	}
	
	@JsonIgnore
	public BiConsumer<ColumnDescriptor, CQOr> getColumnManipulator(){
		return new ColumnManipulator();
	}
	
	@Override
	public void init(Namespaces namespaces, User user) {
		this.setFixedFeatures(prepareConcepts(getQueryGroupId().getDataset().toString()));
		this.formUser = user;
		super.init(namespaces, user);
	}


	@Override
	public Collection<ManagedExecutionId> getUsedQueries() {
		return Collections.singletonList(getQueryGroupId());
	}
	
	private FixedColumn[] prepareConcepts(String datasetName) {
		List<FixedColumn> fixedColumns = new ArrayList<>();
		for(String name : FIXED_COLUMN_NAMES) {
			for(FeatureGroup groupType : asGroupType()) {
				fixedColumns.add(FixedColumn.of(groupType, datasetName, name));
			}
		}
		return fixedColumns.toArray(new FixedColumn[fixedColumns.size()]);
	}

	protected abstract List<FeatureGroup> asGroupType();
	
	@JsonIgnore
	protected abstract ManagedExecutionId getQueryGroupId();
}
