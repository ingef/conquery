package com.bakdata.conquery.models.datasets;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import io.dropwizard.validation.ValidationMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @implNote I am using ids as references here instead of {@link NsIdRef} because I want the PreviewConfig to be pretty soft, instead of having to manage it as a dependent for Concepts and Tables.
 */
@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class PreviewConfig {
	/**
	 * Selects to be used in {@link com.bakdata.conquery.apiv1.QueryProcessor#getSingleEntityExport(Subject, UriBuilder, String, String, List, Dataset, Range)}.
	 * To be displayed as additional infos.
	 *
	 * @implSpec the order of selects is the order of the output fields.
	 */
	@Valid
	private List<InfoCardSelect> infoCardSelects = List.of();

	private Set<ColumnId> hidden = Collections.emptySet();

	private Set<SecondaryIdDescriptionId> grouping = Collections.emptySet();

	private Set<ConnectorId> defaultConnectors = Collections.emptySet();

	public boolean isGroupingColumn(SecondaryIdDescription desc) {
		return getGrouping().contains(desc.getId());
	}

	public boolean isHidden(Column column) {
		return getHidden().contains(column.getId());
	}

	@JacksonInject(useInput = OptBoolean.FALSE)
	@NotNull
	private DatasetRegistry datasetRegistry;

	@Data
	public static class InfoCardSelect {
		@JsonCreator
		public InfoCardSelect(String label, SelectId select) {
			this.label = label;
			this.select = select;
		}

		/**
		 * User facing label of the select.
		 */
		private final String label;
		/**
		 * Id (without dataset) of the select.
		 */
		private final SelectId select;

	}

	@JsonIgnore
	@ValidationMethod(message = "Selects may be used only once.")
	public boolean isSelectsDistinct() {
		return infoCardSelects.stream().map(InfoCardSelect::getSelect).distinct().count() == getInfoCardSelects().size();
	}

	@JsonIgnore
	@ValidationMethod(message = "Select Labels must be unique.")
	public boolean isSelectsLabelsDistinct() {
		return infoCardSelects.stream().map(InfoCardSelect::getLabel).distinct().count() == getInfoCardSelects().size();
	}

	/**
	 * Used to map {@link SelectResultInfo} to {@link InfoCardSelect#getLabel()} via {@link PrintSettings#getColumnNamer()}.
	 */
	public String resolveSelectLabel(SelectResultInfo info) {
		for (InfoCardSelect infoCardSelect : getInfoCardSelects()) {
			if (infoCardSelect.getSelect().equals(info.getSelect().getId())) {
				return infoCardSelect.getLabel();
			}
		}

		throw new IllegalArgumentException(String.format("%s is not an InfoCard Select", info));
	}

	/**
	 * Find infoCard-selects by id within Dataset.
	 */
	@JsonIgnore
	public List<Select> getSelects() {
		return getInfoCardSelects().stream()
								   .map(InfoCardSelect::getSelect)
								   .map(id -> datasetRegistry.findRegistry(id.getDataset()).getOptional(id))
								   .flatMap(Optional::stream)
								   .collect(Collectors.toList());

	}
}
