package com.bakdata.conquery.models.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
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
	private List<InfoCardSelect> infoCardSelects = List.of();

	@Data
	public static class InfoCardSelect {
		/**
		 * User facing label of the select.
		 */
		private final String label;
		/**
		 * Id (without dataset) of the select.
		 *
		 * @implNote this id will be resolved at runtime and cannot be validated at startup.
		 */
		private final String id;
	}

	/**
	 * Used to map {@link SelectResultInfo} to {@link InfoCardSelect#getLabel()} via {@link PrintSettings#getColumnNamer()}.
	 */
	public String resolveSelectLabel(SelectResultInfo info) {
		final String id = info.getSelect().getId().toStringWithoutDataset();

		for (InfoCardSelect infoCardSelect : getInfoCardSelects()) {
			if (infoCardSelect.getId().equals(id)) {
				return infoCardSelect.getLabel();
			}
		}

		throw new IllegalArgumentException(String.format("%s is not an InfoCard Select", info));
	}

	/**
	 * Find infoCard-selects by id within Dataset.
	 */
	public List<Select> resolveInfoCardSelects(Dataset dataset, DatasetRegistry registry) {
		final List<Select> infoCardSelects = new ArrayList<>();

		for (InfoCardSelect select : getInfoCardSelects()) {
			final SelectId selectId = SelectId.Parser.INSTANCE.parsePrefixed(dataset.getName(), select.getId());
			final Select resolved = registry.resolve(selectId);

			infoCardSelects.add(resolved);
		}

		final Set<Select> nonConnectorSelects = infoCardSelects.stream()
															   .filter(select -> !(select.getHolder() instanceof Connector))
															   .collect(Collectors.toSet());

		if (!nonConnectorSelects.isEmpty()) {
			log.error("The selects {} are not connector-Selects", nonConnectorSelects);
			throw new ConqueryError.ExecutionCreationErrorUnspecified();
		}

		return infoCardSelects;

	}
}
