package com.bakdata.conquery.models.execution;

import static com.bakdata.conquery.models.execution.ManagedExecution.AUTO_LABEL_SUFFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.CQAnd;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

public class DefaultLabelTest {
	private static final DatasetRegistry DATASET_REGISTRY = Mockito.mock(DatasetRegistry.class);
	private static final Dataset DATASET = new Dataset() {{
		setName("dataset");
	}};

	private static final TreeConcept CONCEPT = new TreeConcept() {
		{
			setDataset(DATASET);
			setName("defaultconcept");
			setLabel("Default Concept");
		}
	};

	@BeforeAll
	static void beforeAll() {

		I18n.init();

		doAnswer((invocation -> {
			return CONCEPT;

		})).when(DATASET_REGISTRY).resolve(any(ConceptId.class));
	}

	@ParameterizedTest
	@CsvSource({
			"de,Default-Concept-Concept",
			"en,Default-Concept-Concept"
	})
	void autoLabelConceptQuery(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);

		CQConcept concept = makeCQConcept("Concept");
		ConceptQuery cq = new ConceptQuery(concept);
		ManagedQuery mQuery = cq.toManagedExecution(new UserId("User"), DATASET.getId());

		mQuery.setLabel(mQuery.makeAutoLabel(DATASET_REGISTRY, new PrintSettings(true, locale, DATASET_REGISTRY)));

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel + AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}

	private static CQConcept makeCQConcept(String label) {
		CQConcept concept = new CQConcept();
		concept.setLabel(label);
		concept.setElements(List.of(CONCEPT));
		return concept;

	}

	@ParameterizedTest
	@CsvSource({
			"de",
			"en"
	})
	void autoLabelConceptQueryFallback(Locale locale) {
		I18n.LOCALE.set(locale);

		CQConcept concept = new CQConcept();
		concept.setLabel(null);
		ConceptQuery cq = new ConceptQuery(concept);
		ManagedQuery mQuery = cq.toManagedExecution(new UserId("User"), DATASET.getId());
		UUID uuid = UUID.randomUUID();
		mQuery.setQueryId(uuid);

		mQuery.setLabel(mQuery.makeAutoLabel(DATASET_REGISTRY, new PrintSettings(true, locale, DATASET_REGISTRY)));

		assertThat(mQuery.getLabel()).isEqualTo(uuid + AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(uuid.toString());
	}


	@ParameterizedTest
	@CsvSource({
			"de,Anfrage",
			"en,Query"
	})
	void autoLabelReusedQuery(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);

		CQReusedQuery reused = new CQReusedQuery(new ManagedExecutionId(DATASET.getId(), UUID.randomUUID()));
		ConceptQuery cq = new ConceptQuery(reused);
		ManagedQuery mQuery = cq.toManagedExecution(new UserId("User"), DATASET.getId());

		mQuery.setLabel(mQuery.makeAutoLabel(DATASET_REGISTRY, new PrintSettings(true, locale, DATASET_REGISTRY)));

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel + AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}


	@ParameterizedTest
	@CsvSource({
			"de,Hochgeladene-Liste",
			"en,Uploaded-List"
	})
	void autoLabelUploadQuery(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);

		CQExternal external = new CQExternal(List.of(), new String[0][0]);
		ConceptQuery cq = new ConceptQuery(external);
		ManagedQuery mQuery = cq.toManagedExecution(new UserId("User"), DATASET.getId());

		mQuery.setLabel(mQuery.makeAutoLabel(DATASET_REGISTRY, new PrintSettings(true, locale, DATASET_REGISTRY)));

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel + AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}

	@ParameterizedTest
	@CsvSource({
			"de,Hochgeladene-Liste Anfrage Default-Concept-Concept1 Default-Concept-Concept2 und weitere",
			"en,Uploaded-List Query Default-Concept-Concept1 Default-Concept-Concept2 and further"
	})
	void autoLabelComplexQuery(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);

		CQAnd and = new CQAnd();
		CQConcept concept1 = makeCQConcept("Concept1");
		CQConcept concept2 = makeCQConcept("Concept2");
		CQConcept concept3 = makeCQConcept("Concept3veryveryveryveryveryveryveryverylooooooooooooooooooooonglabel");
		and.setChildren(List.of(
				new CQExternal(List.of(), new String[0][0]),
				new CQReusedQuery(new ManagedExecutionId(DATASET.getId(), UUID.randomUUID())),
				concept1,
				concept2,
				concept3
		));
		ConceptQuery cq = new ConceptQuery(and);
		ManagedQuery mQuery = cq.toManagedExecution(new UserId("User"), DATASET.getId());

		mQuery.setLabel(mQuery.makeAutoLabel(DATASET_REGISTRY, new PrintSettings(true, locale, DATASET_REGISTRY)));

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel + AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}


	@ParameterizedTest
	@CsvSource({
			"de,Hochgeladene-Liste Anfrage Default-Concept-Concept2 Default-Concept-Concept3",
			"en,Uploaded-List Query Default-Concept-Concept2 Default-Concept-Concept3"
	})
	void autoLabelComplexQueryNullLabels(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);

		CQAnd and = new CQAnd();
		CQConcept concept1 = new CQConcept();
		concept1.setLabel(null);
		CQConcept concept2 = makeCQConcept("Concept2");
		CQConcept concept3 = makeCQConcept("Concept3");
		and.setChildren(List.of(
				new CQExternal(List.of(), new String[0][0]),
				new CQReusedQuery(new ManagedExecutionId(DATASET.getId(), UUID.randomUUID())),
				concept1,
				concept2,
				concept3
		));
		ConceptQuery cq = new ConceptQuery(and);
		ManagedQuery mQuery = cq.toManagedExecution(new UserId("User"), DATASET.getId());

		mQuery.setLabel(mQuery.makeAutoLabel(DATASET_REGISTRY, new PrintSettings(true, locale, DATASET_REGISTRY)));

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel + AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}

	@ParameterizedTest
	@CsvSource({
			"de,Export 2020-10-30 12:37",
			"en,Export 2020-10-30 12:37"
	})
	void autoLabelExportForm(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);

		ExportForm form = new ExportForm();
		ManagedForm mForm = form.toManagedExecution(new UserId("User"), DATASET.getId());
		mForm.setCreationTime(LocalDateTime.of(2020, 10, 30, 12, 37));

		mForm.setLabel(mForm.makeAutoLabel(DATASET_REGISTRY, new PrintSettings(true, locale, DATASET_REGISTRY)));

		assertThat(mForm.getLabel()).isEqualTo(autoLabel + AUTO_LABEL_SUFFIX);
		assertThat(mForm.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}

}
