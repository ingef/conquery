package com.bakdata.eva.forms.common;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public abstract class StatisticForm extends Form {
	private static final Set<String> STATISTIC_FIELDS = Set.of(
		"title",
		"description",
		"timestamp",
		"user");
	
	private static final FilterProvider FILTERS = new SimpleFilterProvider().addFilter("statisticFilter", SimpleBeanPropertyFilter.filterOutAllExcept(STATISTIC_FIELDS));
	private static final ObjectWriter WRITER = Jackson.MAPPER.copy().addMixIn(StatisticForm.class, StatisticFormMixIn.class).writer().with(FILTERS);

	@JsonInclude
	private String title;
	@JsonInclude
	private String description;
	@Nonnull
	@JsonInclude
	private LocalDateTime timestamp = LocalDateTime.now();
	@Nonnull
	@JsonInclude
	private UserInfo user;

	public StatisticForm(FixedColumn... fixedFeatures) {
		super(fixedFeatures);
	}

	public String toStatisticJSON(Namespaces namespaces) {
		JsonNode n;
		try {
			n = Jackson.MAPPER.readTree(WRITER.writeValueAsBytes(this));
			((ObjectNode) n).set("additionalFields", this.toStatisticView());
			return Jackson.MAPPER.writeValueAsString(n);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void init(Namespaces namespaces, User user) {
		super.init(namespaces, user);
		this.user = new UserInfo(user);
	}

	@JsonIgnore
	public Set<DatasetId> getUsedDatasets(Namespaces namespaces) {
		return this.getUsedQueries().stream().map(ManagedExecutionId::getDataset).distinct().collect(Collectors.toSet());
	}

	protected abstract JsonNode toStatisticView();

	@Override
	public ColumnNamer getColumnNamer() {
		return new ColumnNamer();
	}
}
