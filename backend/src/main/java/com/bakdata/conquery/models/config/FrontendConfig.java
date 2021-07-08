package com.bakdata.conquery.models.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.concept.specific.external.DateColumn;
import com.bakdata.conquery.apiv1.query.concept.specific.external.FormatColumn;
import com.bakdata.conquery.apiv1.query.concept.specific.external.IdColumn;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.util.VersionInfo;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import groovy.transform.ToString;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@ToString
@Getter
@Setter
public class FrontendConfig {

	private String version = VersionInfo.INSTANCE.getProjectVersion();
	@Valid
	@NotNull
	private CurrencyConfig currency = new CurrencyConfig();

	private UploadConfig queryUpload = new UploadConfig();

	@Getter @Setter
	public static class UploadConfig {

		@NotEmpty
		private List<ColumnConfig> ids = List.of(new ColumnConfig(IdColumn.HANDLE, Map.of("en", "Id"), Map.of("en", "Id of the Entity.")));

		@NotNull
		private ColumnConfig dateStart =
				new ColumnConfig(DateColumn.StartDate.HANDLE, Map.of("en", "Begin"), Map.of("en", "Begin of Date-range"));

		@NotNull
		private ColumnConfig dateEnd =
				new ColumnConfig(DateColumn.EndDate.HANDLE, Map.of("en", "Begin"), Map.of("en", "End of Date-range"));

		@NotNull
		private ColumnConfig dateRange =
				new ColumnConfig(DateColumn.DateRange.HANDLE, Map.of("en", "Date Range"), Map.of("en", "Full Date Range"));

		@NotNull
		private ColumnConfig dateSet =
				new ColumnConfig(DateColumn.DateSet.HANDLE, Map.of("en", "Dateset"), Map.of("en", "Set of Date-Ranges"));

		@NotNull
		private ColumnConfig eventDate =
				new ColumnConfig(DateColumn.EventDate.HANDLE, Map.of("en", "Event Date"), Map.of("en", "Single event"));

		@Data
		public static class ColumnConfig {

			/**
			 * Name of the Column, used to resolve the specific entry at {@link IdMappingConfig#getFormatColumns()}
			 */
			@NotEmpty
			private final String name;

			/**
			 * Map of Localized labels.
			 */
			private final Map<String, String> label;

			/**
			 * Map of Localized description.
			 */
			private final Map<String, String> description;

			@JsonIgnore
			@ValidationMethod
			public boolean isExistingColumnFormat() {
				return CPSTypeIdResolver.getImplementation(FormatColumn.class, name) != null;
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