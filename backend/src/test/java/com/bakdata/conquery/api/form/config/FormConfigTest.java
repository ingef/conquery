package com.bakdata.conquery.api.form.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.validation.Validator;

import com.bakdata.conquery.apiv1.FormConfigPatch;
import com.bakdata.conquery.apiv1.forms.FormConfigAPI;
import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.forms.export_form.RelativeMode;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.auth.permissions.FormPermission;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.auth.DevelopmentAuthorizationConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.dropwizard.jersey.validation.Validators;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;


/**
 * Tests the operation of the {@link FormConfigProcessor}.
 * Since the storage is mocked, SERDESing of the {@link FormConfig} is not tested.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class FormConfigTest {

	private ConqueryConfig config = new ConqueryConfig();

	private MetaStorage storage;
	private DatasetRegistry namespacesMock;

	private FormConfigProcessor processor;
	private AuthorizationController controller;
	private Validator validator = Validators.newValidatorFactory().getValidator();

	private Dataset dataset = new Dataset("test");
	private Dataset dataset1 = new Dataset("test1");
	private DatasetId datasetId;
	private DatasetId datasetId1;
	private ExportForm form;
	private User user;

	@BeforeAll
	public void setupTestClass() throws Exception {
		config.getFrontend().setManualUrl(new URL("http://example.org/manual/welcome"));

		datasetId = dataset.getId();
		datasetId1 = dataset1.getId();

		// Mock DatasetRegistry for translation
		namespacesMock = Mockito.mock(DatasetRegistry.class);

		doAnswer(invocation -> {
			throw new UnsupportedOperationException("Not yet implemented");
		}).when(namespacesMock).getOptional(any());

		doAnswer(invocation -> {
			final DatasetId id = invocation.getArgument(0);
			Namespace namespaceMock = Mockito.mock(LocalNamespace.class);
			if (id.equals(datasetId)) {
				when(namespaceMock.getDataset()).thenReturn(dataset);
			}
			else if (id.equals(datasetId1)) {
				when(namespaceMock.getDataset()).thenReturn(dataset1);
			}
			else {
				throw new IllegalStateException("Unknown dataset id.");
			}
			return namespaceMock;
		}).when(namespacesMock).get(any(DatasetId.class));
		when(namespacesMock.getAllDatasets()).thenReturn(List.of(dataset, dataset1));
		when(namespacesMock.injectIntoNew(any(ObjectMapper.class))).thenCallRealMethod();
		when(namespacesMock.inject(any(MutableInjectableValues.class))).thenCallRealMethod();

		storage = new NonPersistentStoreFactory().createMetaStorage();

		((MutableInjectableValues) FormConfigProcessor.getMAPPER().getInjectableValues())
				.add(IdResolveContext.class, namespacesMock);
		processor = new FormConfigProcessor(validator, storage, namespacesMock);
		controller = new AuthorizationController(storage, new DevelopmentAuthorizationConfig());
		controller.start();
	}

	@BeforeEach
	public void setupTest() {

		final ManagedQuery managedQuery = new ManagedQuery(null, null, dataset, null);
		managedQuery.setQueryId(UUID.randomUUID());

		form = new ExportForm();
		AbsoluteMode mode = new AbsoluteMode();
		form.setTimeMode(mode);
		form.setQueryGroupId(managedQuery.getId());
		mode.setForm(form);


		user = new User("test", "test", storage);
		storage.addUser(user);
	}

	@AfterEach
	public void cleanupTest() {
		storage.clear();
	}

	@Test
	public void addConfigWithoutTranslation() {
		user.addPermission(dataset.createPermission(Ability.READ.asSet()));

		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		FormConfigAPI formConfig = FormConfigAPI.builder()
												.formType(form.getFormType())
												.values(mapper.valueToTree(form))
												.build();

		processor.addConfig(user, dataset, formConfig);

		assertThat(storage.getAllFormConfigs()).containsExactly(formConfig.intern(user, dataset));
	}

	@Test
	public void deleteConfig() {
		// PREPARE
		user.addPermission(DatasetPermission.onInstance(Ability.READ, datasetId));

		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), mapper.valueToTree(form));
		formConfig.setDataset(dataset);

		user.addPermission(formConfig.createPermission(AbilitySets.FORM_CONFIG_CREATOR));
		storage.addFormConfig(formConfig);

		// EXECUTE
		processor.deleteConfig(user, formConfig);

		// CHECK
		assertThat(storage.getAllFormConfigs()).doesNotContain(formConfig);

		assertThat(user.getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, formConfig.getId()));
	}

	@Test
	public void getConfig() {
		// PREPARE
		user.addPermission(dataset.createPermission(Ability.READ.asSet()));

		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		formConfig.setDataset(dataset);
		formConfig.setOwner(user);
		user.addPermission(formConfig.createPermission(Ability.READ.asSet()));
		storage.addFormConfig(formConfig);

		// EXECUTE
		FormConfigFullRepresentation response = processor.getConfig(user, formConfig);

		// CHECK
		assertThat(response).usingRecursiveComparison()
							.ignoringFields(FormConfigOverviewRepresentation.Fields.createdAt)
							.isEqualTo(FormConfigFullRepresentation.builder()
																   .formType(form.getClass().getAnnotation(CPSType.class).id())
																   .id(formConfig.getId())
																   .label(formConfig.getLabel())
																   .own(true)
																   .ownerName(user.getLabel())
																   .shared(false)
																   .groups(Collections.emptySet())
																   .system(false)
																   .tags(formConfig.getTags())
																   .values(values)
																   .build());

	}

	@Test
	public void formScannerTest() throws Exception {
		final FormScanner formScanner = new FormScanner(config);

		formScanner.execute(Collections.emptyMap(), null);

		assertThat(FormScanner.FRONTEND_FORM_CONFIGS).containsKeys("TEST_FORM_REL_URL", "TEST_FORM_ABS_URL", "EXPORT_FORM", "FULL_EXPORT_FORM");

		assertThat(FormScanner.FRONTEND_FORM_CONFIGS.get("TEST_FORM_REL_URL").getRawConfig().get("manualUrl").asText())
				.as("relative to base url resolved url")
				.isEqualTo("http://example.org/manual/test-form");

		assertThat(FormScanner.FRONTEND_FORM_CONFIGS.get("TEST_FORM_ABS_URL").getRawConfig().get("manualUrl").asText())
				.as("given absolute url")
				.isEqualTo("http://example.org/absolute-url/test-form");

	}

	@Test
	public void getConfigs() {
		// PREPARE
		user.addPermission(dataset.createPermission(Ability.READ.asSet()));
		user.addPermission(FormPermission.onInstance(Ability.CREATE, form.getFormType()));

		ExportForm form2 = new ExportForm();
		RelativeMode mode3 = new RelativeMode();
		form2.setTimeMode(mode3);
		mode3.setForm(form);
		form.setFeatures(List.of(new CQConcept()));

		TestForm form3 = new TestForm.Abs();

		ObjectMapper mapper = FormConfigProcessor.getMAPPER();

		FormConfigAPI formConfig = FormConfigAPI.builder()
												.formType(form.getFormType())
												.values(mapper.valueToTree(form))
												.build();
		FormConfigAPI formConfig2 = FormConfigAPI.builder()
												 .formType(form2.getFormType())
												 .values(mapper.valueToTree(form2))
												 .build();
		// This should not be retrieved by the user because it does not hold the Permission to create TestForms
		FormConfigAPI formConfig3 = FormConfigAPI.builder()
												 .formType(form3.getFormType())
												 .values(mapper.valueToTree(form2))
												 .build();
		FormConfigId formId = processor.addConfig(user, dataset, formConfig).getId();
		FormConfigId formId2 = processor.addConfig(user, dataset, formConfig2).getId();
		FormConfigId _formId3 = processor.addConfig(user, dataset, formConfig3).getId();


		FormScanner.FRONTEND_FORM_CONFIGS = Map.of(form.getFormType(), new FormType(form.getFormType(), new TextNode("dummy")));

		// EXECUTE
		Stream<FormConfigOverviewRepresentation> response = processor.getConfigsByFormType(user, dataset, Collections.emptySet());

		// CHECK
		assertThat(response).containsExactlyInAnyOrder(
				FormConfigOverviewRepresentation.builder()
												.formType(form.getClass().getAnnotation(CPSType.class).id())
												.id(formId)
												.label(formConfig.getLabel())
												.own(true)
												.ownerName(user.getLabel())
												.shared(false)
												.system(false)
												.tags(formConfig.getTags())
												.createdAt(formConfig.getCreationTime().atZone(ZoneId.systemDefault()))
												.build(),
				FormConfigOverviewRepresentation.builder()
												.formType(form2.getClass().getAnnotation(CPSType.class).id())
												.id(formId2)
												.label(formConfig2.getLabel())
												.own(true)
												.ownerName(user.getLabel())
												.shared(false)
												.system(false)
												.tags(formConfig2.getTags())
												.createdAt(formConfig2.getCreationTime().atZone(ZoneId.systemDefault()))
												.build()
		);
	}

	@Test
	public void patchConfig() {
		// PREPARE
		user.addPermission(DatasetPermission.onInstance(Ability.READ, datasetId));
		Group group1 = new Group("test1", "test1", storage);
		storage.addGroup(group1);
		Group group2 = new Group("test2", "test2", storage);
		storage.addGroup(group2);

		group1.addMember(user);
		group2.addMember(user);

		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		FormConfigAPI formConfig = FormConfigAPI.builder()
												.formType(form.getFormType())
												.values(values)
												.build();
		FormConfig config = processor.addConfig(user, dataset, formConfig);

		// EXECUTE PART 1
		processor.patchConfig(
				user,
				config,
				FormConfigPatch.builder()
							   .label("newTestLabel")
							   .tags(new String[]{"tag1", "tag2"})
							   .groups(List.of(group1.getId()))
							   .values(new ObjectNode(mapper.getNodeFactory(), Map.of("test-Node", new TextNode("test-text"))))
							   .build()
		);

		// CHECK PART 1
		FormConfig patchedFormExpected = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		patchedFormExpected.setDataset(dataset);
		patchedFormExpected.setFormId(config.getFormId());
		patchedFormExpected.setLabel("newTestLabel");
		patchedFormExpected.setShared(true);
		patchedFormExpected.setTags(new String[]{"tag1", "tag2"});
		patchedFormExpected.setOwner(user);
		patchedFormExpected.setValues(new ObjectNode(mapper.getNodeFactory(), Map.of("test-Node", new TextNode("test-text"))));

		final FormConfigId formId = config.getId();
		assertThat(storage.getFormConfig(formId)).usingRecursiveComparison()
												 .ignoringFields("cachedId", FormConfig.Fields.creationTime)
												 .isEqualTo(patchedFormExpected);

		assertThat(storage.getGroup(group1.getId()).getPermissions()).contains(FormConfigPermission.onInstance(AbilitySets.SHAREHOLDER, formId));
		assertThat(storage.getGroup(group2.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.SHAREHOLDER, formId));


		// EXECUTE PART 2 (Unshare)
		processor.patchConfig(
				user,
				config,
				FormConfigPatch.builder()
							   .groups(List.of())
							   .build()
		);

		// CHECK PART 2
		patchedFormExpected.setShared(false);

		assertThat(storage.getFormConfig(formId)).usingRecursiveComparison()
												 .ignoringFields("cachedId", FormConfig.Fields.creationTime)
												 .isEqualTo(patchedFormExpected);

		assertThat(storage.getGroup(group1.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.SHAREHOLDER, formId));
		assertThat(storage.getGroup(group2.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.SHAREHOLDER, formId));
	}

}
