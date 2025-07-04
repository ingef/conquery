package com.bakdata.conquery.models.datasets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.google.common.collect.Sets;
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
	 * Selects to be used in {@link com.bakdata.conquery.apiv1.QueryProcessor#getSingleEntityExport(Subject, UriBuilder, String, String, List, com.bakdata.conquery.models.identifiable.ids.specific.DatasetId, Range)}.
	 * To be displayed as additional infos.
	 *
	 * @implSpec the order of selects is the order of the output fields.
	 */
	@Valid
	private List<InfoCardSelect> infoCardSelects = List.of();

	@Valid
	private List<TimeStratifiedSelects> timeStratifiedSelects = List.of();

	/**
	 * Columns that should not be displayed to users in entity preview.
	 */
	private Set<ColumnId> hidden = Collections.emptySet();

	/**
	 * SecondaryIds where the columns should be grouped together.
	 */
	private Set<SecondaryIdDescriptionId> grouping = Collections.emptySet();

	/**
	 * All Connectors that may be used by users in the EntityPreview.
	 *
	 * @implNote This is purely for the frontend, the backend can theoretically be queried for all Connectors.
	 */
	private Set<ConnectorId> allConnectors = Collections.emptySet();

	/**
	 * Connectors that shall be selected by default by the frontend.
	 */
	private Set<ConnectorId> defaultConnectors = Collections.emptySet();

	/**
	 * Link to list of Filters to provide search capabilities for entities.
	 * <p>
	 * This looks weird at first, but allows reuse of available components instead of introducing duplicate behaviour.
	 * <p>
	 * The Frontend will use the concepts filters to render a search for entity preview.
	 */
	private Set<FilterId> searchFilters = Collections.emptySet();

	@JacksonInject(useInput = OptBoolean.FALSE)
	@NotNull
	private DatasetRegistry<Namespace> datasetRegistry;

	public boolean isGroupingColumn(SecondaryIdDescriptionId descId) {
		return getGrouping().contains(descId);
	}

	public boolean isHidden(ColumnId columnId) {
		return getHidden().contains(columnId);
	}


	/**
	 * @param label  User facing label of the select.
	 * @param select Id (without dataset) of the select.
	 */
	public record InfoCardSelect(@NotNull String label, SelectId select, String description) {
	}


	/**
	 * Defines a group of selects that will be evaluated per quarter and year in the requested period of the entity-preview.
	 */
	public record TimeStratifiedSelects(@NotNull String label, String description, @NotEmpty List<InfoCardSelect> selects) {
	}

	@ValidationMethod(message = "Selects may be referenced only once.")
	@JsonIgnore
	public boolean isSelectsUnique() {
		return timeStratifiedSelects.stream().map(TimeStratifiedSelects::selects).flatMap(Collection::stream).map(InfoCardSelect::select).distinct().count()
			   == timeStratifiedSelects.stream().map(TimeStratifiedSelects::selects).mapToLong(Collection::size).sum();
	}

	@ValidationMethod(message = "Labels must be unique.")
	@JsonIgnore
	public boolean isLabelsUnique() {
		return timeStratifiedSelects.stream().map(TimeStratifiedSelects::selects).flatMap(Collection::stream).map(InfoCardSelect::label).distinct().count()
			   == timeStratifiedSelects.stream().map(TimeStratifiedSelects::selects).mapToLong(Collection::size).sum();
	}

	@JsonIgnore
	@ValidationMethod(message = "Default Connectors must also be available Connectors.")
	public boolean isDefaultSubsetOfAvailable() {
		return Sets.difference(getDefaultConnectors(), getAllConnectors()).isEmpty();
	}

	@JsonIgnore
	@ValidationMethod(message = "Selects may be used only once.")
	public boolean isSelectsDistinct() {
		return infoCardSelects.stream().map(InfoCardSelect::select).distinct().count() == getInfoCardSelects().size();
	}

	@JsonIgnore
	@ValidationMethod(message = "Select Labels must be unique.")
	public boolean isSelectsLabelsDistinct() {
		return infoCardSelects.stream().map(InfoCardSelect::label).distinct().count() == getInfoCardSelects().size();
	}

	@JsonIgnore
	@ValidationMethod(message = "timeStratifiedSelects' labels must be unique.")
	public boolean isStratifiedInfosUnique() {
		return timeStratifiedSelects.stream().map(TimeStratifiedSelects::label).distinct().count() == timeStratifiedSelects.size();
	}


	@JsonIgnore
	@ValidationMethod(message = "SearchFilters must be of same concept.")
	public boolean isSearchFiltersOfSameConcept() {
		return searchFilters.stream().map(id -> id.getConnector().getConcept()).distinct().count() <= 1;
	}

	/**
	 * Used to map {@link SelectResultInfo} to {@link InfoCardSelect#label()} via {@link PrintSettings#getColumnNamer()}.
	 */
	public String resolveSelectLabel(SelectResultInfo info) {
		for (InfoCardSelect infoCardSelect : getInfoCardSelects()) {
			if (infoCardSelect.select().equals(info.getSelect().getId())) {
				return infoCardSelect.label();
			}
		}

		throw new IllegalArgumentException(String.format("%s is not an InfoCard Select", info));
	}

	/**
	 * Find infoCard-selects by id within Dataset.
	 */
	@JsonIgnore
	public List<SelectId> getSelects() {
		return getInfoCardSelects().stream()
								   .map(InfoCardSelect::select)
								   .collect(Collectors.toList());
	}

	public ConceptId resolveSearchConcept() {
		if (searchFilters == null) {
			return null;
		}

		if (searchFilters.isEmpty()) {
			return null;
		}

		return searchFilters.iterator().next().getConnector().getConcept();
	}
}
