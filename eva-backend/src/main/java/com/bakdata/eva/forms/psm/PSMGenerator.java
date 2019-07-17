package com.bakdata.eva.forms.psm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.common.Matched;
import com.bakdata.eva.forms.common.TimeAccessedResult;
import com.bakdata.eva.forms.export.ExportForm;
import com.bakdata.eva.forms.export.ExportGenerator;
import com.bakdata.eva.forms.queries.PSMFormQuery;
import com.bakdata.eva.forms.queries.RelativeFormQuery;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PSMGenerator {

	private final Dataset dataset;
	private final User user;
	private final Namespaces namespaces;
	private final ObjectMapper mapper;

	public PSMGenerator(Dataset dataset, User user, Namespaces namespaces) {
		this.dataset = dataset;
		this.user = user;
		this.namespaces = namespaces;
		this.mapper = namespaces.injectInto(Jackson.MAPPER.copy());
	}

	public List<ManagedQuery> execute(PSMForm form) throws JSONException, IOException {
		ExportGenerator generator = new ExportGenerator(dataset, user, namespaces);
		RelativeFormQuery a = generator.generate(transformForm(form, form.getControlGroup()));
		RelativeFormQuery b = generator.generate(transformForm(form, form.getFeatureGroup()));

		return Arrays.asList(
			namespaces.get(form.getControlGroup().resolveDatasetId()).getQueryManager().createQuery(new PSMFormQuery(a, true), user),
			namespaces.get(form.getFeatureGroup().resolveDatasetId()).getQueryManager().createQuery(new PSMFormQuery(b, false), user)
		);
	}

	private ExportForm transformForm(PSMForm form, TimeAccessedResult group) throws IOException {
		Dataset target = namespaces
			.get(group.resolveDatasetId())
			.getStorage()
			.getDataset();
		
		group.setId(new ManagedExecutionId(target.getId(), group.getId().getExecution()));

		ExportForm result = new ExportForm();
		result.setColumns(form.getColumns());
		result.setFeatures(translate(form.getFeatures(), target));
		result.setOutcomes(translate(form.getOutcomes(), target));
		result.setId(UUID.randomUUID());
		result.setIndexDate(form.getIndexDate());
		result.setQueryGroup(group);
		result.setTimeCountAfter(form.getTimeCountAfter());
		result.setTimeCountBefore(form.getTimeCountBefore());
		result.setTimeUnit(form.getTimeUnit());
		return result;
	}

	private List<CQOr> translate(List<Matched> features, Dataset target) throws IOException {
		List<CQOr> result = new ArrayList<>(features.size());
		for (Matched feature : features) {
			result.add(translate(feature, target));
		}
		return result;
	}
	
	private CQOr translate(CQOr feature, Dataset target) throws IOException {
		String value = Jackson.MAPPER.writeValueAsString(feature);

		Pattern[] patterns = feature
			.collectNamespacedIds()
			.stream()
			.map(NamespacedId::getDataset)
			.map(DatasetId::toString)
			.map(n -> Pattern.compile("(?<=(\"))" + Pattern.quote(n) + "(?=(\\.|\"))"))
			.toArray(Pattern[]::new);

		String replacement = Matcher.quoteReplacement(target.getId().toString());
		for (Pattern pattern : patterns) {
			value = pattern.matcher(value).replaceAll(replacement);
		}

		return mapper.readValue(value, CQOr.class);
	}
}
