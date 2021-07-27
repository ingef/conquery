package com.bakdata.conquery.models.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.concept.specific.external.DateFormat;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.mapping.AutoIncrementingPseudomizer;
import com.bakdata.conquery.models.identifiable.mapping.FullIdPrinter;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.VersionInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import groovy.transform.ToString;
import io.dropwizard.validation.ValidationMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

@ToString
@Getter
@Setter
@With
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class FrontendConfig {

	private String version = VersionInfo.INSTANCE.getProjectVersion();
	@Valid
	@NotNull
	private CurrencyConfig currency = new CurrencyConfig();

	private UploadConfig queryUpload = new UploadConfig();

	@Getter
	@Setter
	@With
	@AllArgsConstructor
	@NoArgsConstructor
	public static class UploadConfig {

		@NotEmpty
		@Valid
		private List<ColumnConfig> ids = List.of(
				ColumnConfig.builder()
							.name("ID")
							.field("id")
							.resolvable(true)
							.fillAnon(true)
							.build()
		);

		@JsonIgnore
		private List<String> idFieldsCached;

		/**
		 * Headers for Output CSV.
		 */
		@JsonIgnore
		public List<String> getPrintIdFields() {
			if (idFieldsCached == null) {
				idFieldsCached = ids.stream()
									.map(ColumnConfig::getField)
									.filter(Objects::nonNull)
									.collect(Collectors.toList());
			}

			return idFieldsCached;
		}

		public ColumnConfig getIdMapper(String name) {
			return ids.stream()
					  .filter(mapper -> mapper.getName().equals(name)) //TODO use map
					  .findFirst()
					  .orElse(null);
		}

		public int getIdIndex(List<String> format) {
			for (int index = 0; index < format.size(); index++) {
				final String current = format.get(index);

				if (getIdMapper(current) != null) {
					return index;
				}
			}

			return -1;
		}

		@NotNull
		@Valid
		private ColumnConfig dateStart = ColumnConfig.builder()
													 .name(DateFormat.START_DATE.name())
													 .label(Map.of("en", "Begin"))
													 .description(Map.of("en", "Begin of Date-range"))
													 .build();

		@NotNull
		@Valid
		private ColumnConfig dateEnd = ColumnConfig.builder()
												   .name(DateFormat.END_DATE.name())
												   .label(Map.of("en", "End"))
												   .description(Map.of("en", "End of Date-range"))
												   .build();


		@NotNull
		@Valid
		private ColumnConfig dateRange = ColumnConfig.builder()
													 .name(DateFormat.DATE_RANGE.name())
													 .label(Map.of("en", "Date Range"))
													 .description(Map.of("en", "Full Date Range"))
													 .build();


		@NotNull
		@Valid
		private ColumnConfig dateSet = ColumnConfig.builder()
												   .name(DateFormat.DATE_SET.name())
												   .label(Map.of("en", "Dateset"))
												   .description(Map.of("en", "Set of Date-Ranges"))
												   .build();


		@NotNull
		@Valid
		private ColumnConfig eventDate = ColumnConfig.builder()
													 .name(DateFormat.EVENT_DATE.name())
													 .label(Map.of("en", "Event Date"))
													 .description(Map.of("en", "Single event"))
													 .build();

		@ValidationMethod(message = "Duplicate Claims for Mapping Columns.")
		@JsonIgnore
		public boolean isAllColsUnique() {
			Map<String, ColumnConfig> dupes = new HashMap<>();

			final ArrayList<ColumnConfig> candidates = new ArrayList<>(ids);
			candidates.addAll(List.of(dateStart, dateEnd, dateSet, dateRange, eventDate));

			for (ColumnConfig config : candidates) {
				final ColumnConfig prior = dupes.put(config.getName(), config);

				if (prior == null) {
					continue;
				}

				log.error("Duplicate claims for Name = `{}` ({} / {})", config.getName(), config, prior);
				return false;
			}

			return true;
		}

		@ValidationMethod(message = "Not all Id-Columns have mappings.")
		@JsonIgnore
		public boolean isIdColsHaveMapping() {
			return ids.stream().map(ColumnConfig::getField).allMatch(Objects::nonNull);
		}

		@ValidationMethod(message = "Must have exactly one Column for Pseudomization.")
		@JsonIgnore
		public boolean isExactlyOnePseudo() {
			return ids.stream()
					  .filter(conf -> conf.getField() != null)
					  .filter(ColumnConfig::isFillAnon)
					  .count() == 1;
		}


		public DateFormat resolveDateFormat(String handle) {
			try {
				return DateFormat.valueOf(handle);
			}
			catch (IllegalArgumentException e) {
				return null; // Does not exist
			}
		}


		public IdPrinter getIdPrinter(User owner, ManagedExecution<?> execution, Namespace namespace) {

			if (owner.isPermitted(execution.getDataset(), Ability.PRESERVE_ID)) {
				return new FullIdPrinter(namespace.getStorage().getPrimaryDictionary(), namespace.getStorage().getIdMapping());
			}

			final int size = getPrintIdFields().size();
			final int pos = IntStream.range(0, getIds().size())
									 .filter(idx -> getIds().get(idx).isFillAnon())
									 .findFirst()
									 .orElseThrow();

			return new AutoIncrementingPseudomizer(size, pos);
		}
	}

	@Data
	public static class CurrencyConfig {
		private String prefix = "€";
		private String thousandSeparator = ".";
		private String decimalSeparator = ",";
		private int decimalScale = 2;
	}


}