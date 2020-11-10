package com.bakdata.conquery.models.execution;
import static com.bakdata.conquery.models.execution.ManagedExecution.AUTO_LABEL_SUFFIX;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.CQAnd;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class DefaultLabelTest {
	private static final DatasetRegistry DATASET_REGISTRY = new DatasetRegistry(0);
	private static final Dataset DATASET = new Dataset();
	
	@BeforeAll
	static void beforeAll() {
		I18n.init();
	}
	
	@ParameterizedTest
	@CsvSource({
		"de,Concept",
		"en,Concept"})
	void autoLabelConceptQuery(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);
		
		CQConcept concept = new CQConcept();
		concept.setLabel("Concept");
		ConceptQuery cq = new ConceptQuery(concept);
		ManagedQuery mQuery = cq.toManagedExecution(DATASET_REGISTRY, new UserId("User"), DATASET.getId());

		mQuery.setLabel(mQuery.makeAutoLabel());
		
		assertThat(mQuery.getLabel()).isEqualTo(autoLabel+AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}
	
	@ParameterizedTest
	@CsvSource({
		"de",
		"en"})
	void autoLabelConceptQueryFallback(Locale locale) {
		I18n.LOCALE.set(locale);
		
		CQConcept concept = new CQConcept();
		concept.setLabel(null);
		ConceptQuery cq = new ConceptQuery(concept);
		ManagedQuery mQuery = cq.toManagedExecution(DATASET_REGISTRY, new UserId("User"), DATASET.getId());
		UUID uuid = UUID.randomUUID();
		mQuery.setQueryId(uuid);

		mQuery.setLabel(mQuery.makeAutoLabel());
		
		assertThat(mQuery.getLabel()).isEqualTo(uuid+AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(uuid.toString());
	}
	
	
	@ParameterizedTest
	@CsvSource({
		"de,Frühere-Anfrage",
		"en,Previous-Query"})
	void autoLabelReusedQuery(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);
		
		CQReusedQuery reused = new CQReusedQuery(new ManagedExecutionId(DATASET.getId(), UUID.randomUUID()));
		ConceptQuery cq = new ConceptQuery(reused);
		ManagedQuery mQuery = cq.toManagedExecution(DATASET_REGISTRY, new UserId("User"), DATASET.getId());

		mQuery.setLabel(mQuery.makeAutoLabel());

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel+AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}
	
	
	@ParameterizedTest
	@CsvSource({
		"de,Hochgeladene-Liste",
		"en,Uploaded-List"})
	void autoLabelUploadQuery(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);
		
		CQExternal external = new CQExternal(List.of(), new String[0][0]);
		ConceptQuery cq = new ConceptQuery(external);
		ManagedQuery mQuery = cq.toManagedExecution(DATASET_REGISTRY, new UserId("User"), DATASET.getId());

		mQuery.setLabel(mQuery.makeAutoLabel());

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel+AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}
	
	@ParameterizedTest
	@CsvSource({
		"de,Hochgeladene-Liste Frühere-Anfrage Concept1-Concept2",
		"en,Uploaded-List Previous-Query Concept1-Concept2"})
	void autoLabelComplexQuery(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);
		
		CQAnd and = new CQAnd();
		CQConcept concept1 = new CQConcept();
		concept1.setLabel("Concept1");
		CQConcept concept2 = new CQConcept();
		concept2.setLabel("Concept2");
		CQConcept concept3 = new CQConcept();
		concept3.setLabel("Concept3");
		and.setChildren(List.of(
			new CQExternal(List.of(), new String[0][0]),
			new CQReusedQuery(new ManagedExecutionId(DATASET.getId(), UUID.randomUUID())),
			concept1,
			concept2,
			concept3
			));
		ConceptQuery cq = new ConceptQuery(and);
		ManagedQuery mQuery = cq.toManagedExecution(DATASET_REGISTRY, new UserId("User"), DATASET.getId());

		mQuery.setLabel(mQuery.makeAutoLabel());

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel+AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}
	
	
	@ParameterizedTest
	@CsvSource({
		"de,Hochgeladene-Liste Frühere-Anfrage Concept2-Concept3",
		"en,Uploaded-List Previous-Query Concept2-Concept3"})
	void autoLabelComplexQueryNullLabels(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);
		
		CQAnd and = new CQAnd();
		CQConcept concept1 = new CQConcept();
		concept1.setLabel(null);
		CQConcept concept2 = new CQConcept();
		concept2.setLabel("Concept2");
		CQConcept concept3 = new CQConcept();
		concept3.setLabel("Concept3");
		and.setChildren(List.of(
			new CQExternal(List.of(), new String[0][0]),
			new CQReusedQuery(new ManagedExecutionId(DATASET.getId(), UUID.randomUUID())),
			concept1,
			concept2,
			concept3
			));
		ConceptQuery cq = new ConceptQuery(and);
		ManagedQuery mQuery = cq.toManagedExecution(DATASET_REGISTRY, new UserId("User"), DATASET.getId());

		mQuery.setLabel(mQuery.makeAutoLabel());

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel+AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}
	
	@ParameterizedTest
	@CsvSource({
		"de,Export 2020-10-30 12:37",
		"en,Export 2020-10-30 12:37"})
	void autoLabelExportForm(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);
		
		ExportForm form = new ExportForm();
		ManagedForm mForm = form.toManagedExecution(DATASET_REGISTRY, new UserId("User"), DATASET.getId());
		mForm.setCreationTime(LocalDateTime.of(2020, 10, 30, 12, 37));

		mForm.setLabel(mForm.makeAutoLabel());
		
		assertThat(mForm.getLabel()).isEqualTo(autoLabel+AUTO_LABEL_SUFFIX);
		assertThat(mForm.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}

}
