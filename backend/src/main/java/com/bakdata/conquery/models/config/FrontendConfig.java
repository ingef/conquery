package com.bakdata.conquery.models.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import com.google.common.base.Functions;
import groovy.transform.ToString;
import io.dropwizard.validation.ValidationMethod;
import lombok.AccessLevel;
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
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@With
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
							.field("result")
							.resolvable(true)
							.fillAnon(true)
							.build()
		);

		@JsonIgnore
		@Setter(AccessLevel.NONE)
		@Getter(AccessLevel.NONE)
		private List<String> idFieldsCached;

		/**
		 * Headers for Output CSV.
		 */
		@JsonIgnore
		public List<String> getPrintIdFields() {
			if (idFieldsCached == null) {
				idFieldsCached = ids.stream()
									.filter(ColumnConfig::isPrint)
									.map(ColumnConfig::getField)
									.collect(Collectors.toUnmodifiableList());
			}

			return idFieldsCached;
		}

		@JsonIgnore
		@Setter(AccessLevel.NONE)
		private Map<String, ColumnConfig> idMappers;

		public ColumnConfig getIdMapper(String name) {
			if (idMappers == null) {
				idMappers = ids.stream().filter(ColumnConfig::isResolvable)
							   .collect(Collectors.toMap(ColumnConfig::getName, Functions.identity()));
			}

			return idMappers.get(name);
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


		@ValidationMethod(message = "Duplicate Claims for Mapping Columns.")
		@JsonIgnore
		public boolean isAllColsUnique() {
			Set<String> dupes = new HashSet<>();

			final List<String> candidates = new ArrayList<>();

			ids.stream().map(ColumnConfig::getName).forEach(candidates::add);
			Arrays.stream(DateFormat.values()).map(DateFormat::name).forEach(candidates::add);

			for (String name : candidates) {
				if (dupes.add(name)) {
					continue;
				}

				log.error("Duplicate claims for Name = `{}`", name);
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


		/**
		 * Try to create a {@link FullIdPrinter} for user if they are allowed. If not allowed to read ids, they will receive ab pseudomized result instead.
		 */
		public IdPrinter getIdPrinter(User owner, ManagedExecution<?> execution, Namespace namespace) {
			final int size = getPrintIdFields().size();
			final int pos = IntStream.range(0, getIds().size())
									 .filter(idx -> getIds().get(idx).isFillAnon())
									 .findFirst()
									 .orElseThrow();

			if (owner.isPermitted(execution.getDataset(), Ability.PRESERVE_ID)) {
				return new FullIdPrinter(namespace.getStorage().getPrimaryDictionary(), namespace.getStorage().getIdMapping(), size, pos);
			}



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