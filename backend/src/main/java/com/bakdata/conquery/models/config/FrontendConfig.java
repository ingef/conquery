package com.bakdata.conquery.models.config;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.concept.specific.external.DateColumn;
import com.bakdata.conquery.apiv1.query.concept.specific.external.DateFormat;
import com.bakdata.conquery.util.VersionInfo;
import groovy.transform.ToString;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

@ToString
@Getter
@Setter
@With
@AllArgsConstructor
@NoArgsConstructor
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
		private List<ColumnConfig> ids;

		@NotNull
		private ColumnConfig dateStart = ColumnConfig.builder()
													 .name(DateColumn.StartDate.HANDLE)
													 .label(Map.of("en", "Begin"))
													 .description(Map.of("en", "Begin of Date-range"))
													 .build();

		@NotNull
		private ColumnConfig dateEnd = ColumnConfig.builder()
												   .name(DateColumn.EndDate.HANDLE)
												   .label(Map.of("en", "End"))
												   .description(Map.of("en", "End of Date-range"))
												   .build();


		@NotNull
		private ColumnConfig dateRange = ColumnConfig.builder()
													 .name(DateColumn.DateRange.HANDLE)
													 .label(Map.of("en", "Date Range"))
													 .description(Map.of("en", "Full Date Range"))
													 .build();


		@NotNull
		private ColumnConfig dateSet = ColumnConfig.builder()
												   .name(DateColumn.DateSet.HANDLE)
												   .label(Map.of("en", "Dateset"))
												   .description(Map.of("en", "Set of Date-Ranges"))
												   .build();


		@NotNull
		private ColumnConfig eventDate = ColumnConfig.builder()
													 .name(DateColumn.EventDate.HANDLE)
													 .label(Map.of("en", "Event Date"))
													 .description(Map.of("en", "Single event"))
													 .build();

		public DateFormat resolveDateFormat(String handle) {
			return DateFormat.ALL; //TODO
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