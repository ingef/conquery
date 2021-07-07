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
import com.bakdata.conquery.util.VersionInfo;
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

	@Data
	public static class UploadConfig {
		private final static List<ColumnConfig> DEFAULT_IDS = new ArrayList<>();


		static {
			// Collect all implementations of IdClass.

			final Set<Class<? extends FormatColumn>> idClasses = CPSTypeIdResolver.listImplementations(FormatColumn.class);

			for (Class<? extends FormatColumn> idClass : idClasses) {

				if (!IdColumn.class.isAssignableFrom(idClass)) {
					continue;
				}

				final String id = idClass.getAnnotation(CPSType.class).id();
				DEFAULT_IDS.add(new ColumnConfig(id, Map.of("en", id), Map.of("en", id)));
			}
		}

		@NotEmpty
		private List<ColumnConfig> ids = DEFAULT_IDS;

		@NotNull
		private ColumnConfig dateStart =
				new ColumnConfig(DateColumn.DateStart.class.getAnnotation(CPSType.class).id(), Map.of("en", "Begin"), Map.of("en", "Begin of Date"));

		@NotNull
		private ColumnConfig dateEnd =
				new ColumnConfig(DateColumn.DateEnd.class.getAnnotation(CPSType.class).id(), Map.of("en", "Begin"), Map.of("en", "Begin of Date"));

		@NotNull
		private ColumnConfig dateRange =
				new ColumnConfig(DateColumn.DateRange.class.getAnnotation(CPSType.class).id(), Map.of("en", "Date Range"), Map.of("en", "Full Date Range"));

		@NotNull
		private ColumnConfig dateSet =
				new ColumnConfig(DateColumn.DateSet.class.getAnnotation(CPSType.class).id(), Map.of("en", "Dateset"), Map.of("en", "Set of Date-Ranges"));

		@NotNull
		private ColumnConfig eventDate =
				new ColumnConfig(DateColumn.EventDate.class.getAnnotation(CPSType.class).id(), Map.of("en", "Event Date"), Map.of("en", "Single day"));

		@Data
		public static class ColumnConfig {
			@NotEmpty
			private final String name;

			private final Map<String, String> label;
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