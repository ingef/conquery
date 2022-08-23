package com.bakdata.conquery.models.datasets;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

	@NsIdRefCollection
	private Set<Column> hidden = Collections.emptySet();

	@NsIdRefCollection
	private Set<SecondaryIdDescription> grouping = Collections.emptySet();

	@NsIdRefCollection
	private Set<Connector> defaultConnectors = Collections.emptySet();


	@Data
	public static class InfoCardSelect {
		@JsonCreator
		public InfoCardSelect(String label, @NsIdRef Select select) {
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
		@NsIdRef
		private final Select select;

		@ValidationMethod(message = "Select must be for connectors.")
		@JsonIgnore
		public boolean isConnectorSelect() {
			return select.getHolder() instanceof Connector;
		}
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
			if (infoCardSelect.getSelect().equals(info.getSelect())) {
				return infoCardSelect.getLabel();
			}
		}

		throw new IllegalArgumentException(String.format("%s is not an InfoCard Select", info));
	}

	/**
	 * Find infoCard-selects by id within Dataset.
	 */
	public List<Select> getSelects() {
		return getInfoCardSelects().stream()
								   .map(InfoCardSelect::getSelect)
								   .collect(Collectors.toList());

	}
}
