package com.bakdata.conquery.models.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.concept.specific.external.DateFormat;
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
		private List<ColumnConfig> ids = List.of(
				ColumnConfig.builder()
							.name("ID")
							.mapping(ColumnConfig.Mapping.builder()
														 .field("id")
														 .resolvable(true)
														 .build())
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
									.map(ColumnConfig::getMapping)
									.filter(Objects::nonNull)
									.map(ColumnConfig.Mapping::getField)
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

				if (ids.stream().map(ColumnConfig::getName).anyMatch(current::equals)) {
					return index;
				}
			}

			return -1;
		}

		@NotNull
		private ColumnConfig dateStart = ColumnConfig.builder()
													 .name(DateFormat.START_DATE.name())
													 .label(Map.of("en", "Begin"))
													 .description(Map.of("en", "Begin of Date-range"))
													 .build();

		@NotNull
		private ColumnConfig dateEnd = ColumnConfig.builder()
												   .name(DateFormat.END_DATE.name())
												   .label(Map.of("en", "End"))
												   .description(Map.of("en", "End of Date-range"))
												   .build();


		@NotNull
		private ColumnConfig dateRange = ColumnConfig.builder()
													 .name(DateFormat.DATE_RANGE.name())
													 .label(Map.of("en", "Date Range"))
													 .description(Map.of("en", "Full Date Range"))
													 .build();


		@NotNull
		private ColumnConfig dateSet = ColumnConfig.builder()
												   .name(DateFormat.DATE_SET.name())
												   .label(Map.of("en", "Dateset"))
												   .description(Map.of("en", "Set of Date-Ranges"))
												   .build();


		@NotNull
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
			return ids.stream().map(ColumnConfig::getMapping).allMatch(Objects::nonNull);
		}

		public DateFormat resolveDateFormat(String handle) {
			try {
				return DateFormat.valueOf(handle);
			}
			catch (IllegalArgumentException e) {
				return null; // Does not exist
			}
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