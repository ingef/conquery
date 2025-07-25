package com.bakdata.conquery.models.execution;

import static com.bakdata.conquery.models.execution.ManagedExecution.AUTO_LABEL_SUFFIX;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.TestNamespacedStorageProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

public class DefaultLabelTest {

	public static final ConqueryConfig CONFIG = new ConqueryConfig();
	private final static MetaStorage META_STORAGE = new NonPersistentStoreFactory().createMetaStorage();
	private final static NamespaceStorage NAMESPACE_STORAGE = new NonPersistentStoreFactory().createNamespaceStorage();
	public static final NamespacedStorageProvider STORAGE_PROVIDER = new TestNamespacedStorageProvider(NAMESPACE_STORAGE);
	private static final Namespace NAMESPACE = Mockito.mock(LocalNamespace.class);
	private static final Dataset DATASET = new Dataset("dataset");
	private static final User user = new User("user", "user", META_STORAGE);
	private static final TreeConcept CONCEPT = new TreeConcept();

	@BeforeAll
	public static void beforeAll() throws Exception {
		DATASET.setStorageProvider(STORAGE_PROVIDER);

		NAMESPACE_STORAGE.updateDataset(DATASET);


		// no mapper required
		META_STORAGE.openStores(null);

		CONCEPT.setNamespacedStorageProvider(NAMESPACE_STORAGE);
		CONCEPT.init();
		CONCEPT.setName("defaultconcept");
		CONCEPT.setLabel("Default Concept");


		NAMESPACE_STORAGE.updateConcept(CONCEPT);

		I18n.init();
	}

	@ParameterizedTest
	@CsvSource({
			"de,Concept",
			"en,Concept"
	})
	void autoLabelConceptQuery(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);

		CQConcept concept = makeCQConceptWithLabel("Concept");
		ConceptQuery cq = new ConceptQuery(concept);
		ManagedQuery mQuery = cq.toManagedExecution(user.getId(), DATASET.getId(), META_STORAGE, null, CONFIG);

		mQuery.setLabel(mQuery.makeAutoLabel(getPrintSettings(locale)));

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel + AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}

	private static CQConcept makeCQConceptWithLabel(String label) {
		CQConcept concept = new CQConcept();
		concept.setLabel(label);
		concept.setElements(List.of(CONCEPT.getId()));
		return concept;

	}

	@NotNull
	private PrintSettings getPrintSettings(Locale locale) {
		return new PrintSettings(true, locale, NAMESPACE, CONFIG, null, null);
	}

	@ParameterizedTest
	@CsvSource({
			"de,Default-Concept",
			"en,Default-Concept"
	})
	void autoLabelConceptQueryFallback(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);

		CQConcept concept = new CQConcept();

		concept.setLabel(null);
		concept.setElements(List.of(CONCEPT.getId()));
		ConceptQuery cq = new ConceptQuery(concept);
		ManagedQuery mQuery = cq.toManagedExecution(user.getId(), DATASET.getId(), META_STORAGE, null, CONFIG);
		mQuery.setQueryId(UUID.randomUUID());
		mQuery.setMetaStorage(META_STORAGE);

		mQuery.setLabel(mQuery.makeAutoLabel(getPrintSettings(locale)));

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel + AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}


	@ParameterizedTest
	@CsvSource({
			"de,Anfrage",
			"en,Query"
	})
	void autoLabelReusedQuery(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);

		final ManagedQuery managedQuery = new ManagedQuery(null, new UserId("test"), DATASET.getId(), META_STORAGE, null, CONFIG);
		managedQuery.setQueryId(UUID.randomUUID());

		CQReusedQuery reused = new CQReusedQuery(managedQuery.getId());
		ConceptQuery cq = new ConceptQuery(reused);
		ManagedQuery mQuery = cq.toManagedExecution(user.getId(), DATASET.getId(), META_STORAGE, null, CONFIG);

		mQuery.setLabel(mQuery.makeAutoLabel(getPrintSettings(locale)));

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

		CQExternal external = new CQExternal(List.of(), new String[0][0], false);
		ConceptQuery cq = new ConceptQuery(external);
		ManagedQuery mQuery = cq.toManagedExecution(user.getId(), DATASET.getId(), META_STORAGE, null, CONFIG);

		mQuery.setLabel(mQuery.makeAutoLabel(getPrintSettings(locale)));

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel + AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}

	@ParameterizedTest
	@CsvSource({
			"de,Hochgeladene-Liste Anfrage Concept1 Concept2 und weitere",
			"en,Uploaded-List Query Concept1 Concept2 and further"
	})
	void autoLabelComplexQuery(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);

		final ManagedQuery managedQuery = new ManagedQuery(null, new UserId("test"), DATASET.getId(), META_STORAGE, null, CONFIG);
		managedQuery.setMetaStorage(META_STORAGE);

		managedQuery.setQueryId(UUID.randomUUID());

		CQAnd and = new CQAnd();
		CQConcept concept1 = makeCQConceptWithLabel("Concept1");
		CQConcept concept2 = makeCQConceptWithLabel("Concept2");
		CQConcept concept3 = makeCQConceptWithLabel("Concept3veryveryveryveryveryveryveryverylooooooooooooooooooooonglabel");

		and.setChildren(List.of(
				new CQExternal(List.of(), new String[0][0], false),
				new CQReusedQuery(managedQuery.getId()),
				concept1,
				concept2,
				concept3
		));
		ConceptQuery cq = new ConceptQuery(and);
		ManagedQuery mQuery = cq.toManagedExecution(user.getId(), DATASET.getId(), META_STORAGE, null, CONFIG);

		mQuery.setLabel(mQuery.makeAutoLabel(getPrintSettings(locale)));

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel + AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}


	@ParameterizedTest
	@CsvSource({
			"de,Hochgeladene-Liste Anfrage Default-Concept Concept2 Concept3",
			"en,Uploaded-List Query Default-Concept Concept2 Concept3"
	})
	void autoLabelComplexQueryNullLabels(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);

		final ManagedQuery managedQuery = new ManagedQuery(null, new UserId("test"), DATASET.getId(), META_STORAGE, null, CONFIG);
		managedQuery.setQueryId(UUID.randomUUID());

		CQAnd and = new CQAnd();
		CQConcept concept1 = new CQConcept();
		concept1.setLabel(null);
		concept1.setElements(List.of(CONCEPT.getId()));
		CQConcept concept2 = makeCQConceptWithLabel("Concept2");
		CQConcept concept3 = makeCQConceptWithLabel("Concept3");
		and.setChildren(List.of(
				new CQExternal(List.of(), new String[0][0], false),
				new CQReusedQuery(managedQuery.getId()),
				concept1,
				concept2,
				concept3
		));
		ConceptQuery cq = new ConceptQuery(and);
		ManagedQuery mQuery = cq.toManagedExecution(user.getId(), DATASET.getId(), META_STORAGE, null, CONFIG);

		mQuery.setLabel(mQuery.makeAutoLabel(getPrintSettings(locale)));

		assertThat(mQuery.getLabel()).isEqualTo(autoLabel + AUTO_LABEL_SUFFIX);
		assertThat(mQuery.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}

	@ParameterizedTest
	@CsvSource(value = {
			"de;Datenexport",
			"en;Data Export"
	}, delimiter = ';')
	void autoLabelExportForm(Locale locale, String autoLabel) {
		I18n.LOCALE.set(locale);

		ExportForm form = new ExportForm();
		ManagedForm<?> mForm = form.toManagedExecution(user.getId(), DATASET.getId(), META_STORAGE, null, CONFIG);
		mForm.setCreationTime(LocalDateTime.of(2020, 10, 30, 12, 37));

		mForm.setLabel(mForm.makeAutoLabel(getPrintSettings(locale)));

		assertThat(mForm.getLabel()).isEqualTo(autoLabel + AUTO_LABEL_SUFFIX);
		assertThat(mForm.getLabelWithoutAutoLabelSuffix()).isEqualTo(autoLabel);
	}

}
