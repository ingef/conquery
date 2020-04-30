package com.bakdata.conquery.api.form.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.forms.FormConfig;
import com.bakdata.conquery.apiv1.forms.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.apiv1.forms.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.forms.export_form.RelativeMode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.develop.DevelopmentAuthorizationConfig;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.map.HashedMap;
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
	
	private MasterMetaStorage storageMock;
	
	private Map<FormConfigId, FormConfig> configs = new HashedMap<>();
	private Map<UserId, User> users = new HashedMap<>();
	private Map<GroupId, Group> groups = new HashedMap<>();
	
	private FormConfigProcessor processor;
	private AuthorizationController controller;
	
	private ExportForm form;
	
	@BeforeAll
	public void setupTestClass() throws Exception{
		storageMock = Mockito.mock(MasterMetaStorage.class);

		// Mock Configs
		doAnswer(invocation -> {
			final FormConfigId id = invocation.getArgument(0);
			return configs.get(id);
		}).when(storageMock).getFormConfig(any());
		doAnswer(invocation -> {
			final FormConfig elem = invocation.getArgument(0);
			configs.put(elem.getId(),elem);
			return null;
		}).when(storageMock).updateFormConfig(any());
		doAnswer(invocation -> {
			final FormConfigId id = invocation.getArgument(0);
			configs.remove(id);
			return null;
		}).when(storageMock).removeFormConfig(any());
		when(storageMock.getAllFormConfigs()).thenReturn(configs.values());
		
		// Mock User
		doAnswer(invocation -> {
			final UserId id = invocation.getArgument(0);
			return users.get(id);
		}).when(storageMock).getUser(any());
		doAnswer(invocation -> {
			final User elem = invocation.getArgument(0);
			users.put(elem.getId(),elem);
			return null;
		}).when(storageMock).updateUser(any());
		doAnswer(invocation -> {
			final UserId id = invocation.getArgument(0);
			users.remove(id);
			return null;
		}).when(storageMock).removeUser(any());
		when(storageMock.getAllUsers()).thenReturn(users.values());
		
		// Mock Groups
		doAnswer(invocation -> {
			final GroupId id = invocation.getArgument(0);
			return groups.get(id);
		}).when(storageMock).getGroup(any());
		doAnswer(invocation -> {
			final Group elem = invocation.getArgument(0);
			groups.put(elem.getId(),elem);
			return null;
		}).when(storageMock).updateGroup(any());
		doAnswer(invocation -> {
			final GroupId id = invocation.getArgument(0);
			groups.remove(id);
			return null;
		}).when(storageMock).removeGroup(any());
		when(storageMock.getAllGroups()).thenReturn(groups.values());
		
		
		processor = new FormConfigProcessor(storageMock);
		controller = new AuthorizationController(new DevelopmentAuthorizationConfig(), Collections.emptyList(), storageMock);
		controller.init();
		controller.start();
	}
	
	@BeforeEach
	public void setupTest(){
		configs.clear();
		users.clear();
		groups.clear();

		form = new ExportForm();
		AbsoluteMode mode = new AbsoluteMode();
		form.setTimeMode(mode);
		mode.setForm(form);
		mode.setFeatures(List.of(new CQConcept()));
	}
	
	@Test
	public void addConfig() {
		User user = new User("test","test");
		storageMock.addUser(user);
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), mapper.valueToTree(form));
		processor.addConfig(user, formConfig);
		
		assertThat(configs).containsAllEntriesOf(Map.of(formConfig.getId(),formConfig));
		
		assertThat(users.get(user.getId()).getPermissions()).contains(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, formConfig.getId()));
	}
	
	@Test
	public void deleteConfig() {
		// PREPARE
		User user = new User("test","test");
		storageMock.addUser(user);
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), mapper.valueToTree(form));
		
		user.addPermission(storageMock, FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, formConfig.getId()));
		configs.put(formConfig.getId(),formConfig);
		
		// EXECUTE
		processor.deleteConfig(user, formConfig.getId());
		
		// CHECK
		assertThat(configs).doesNotContainEntry(formConfig.getId(),formConfig);
		
		assertThat(users.get(user.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, formConfig.getId()));
	}
	
	@Test
	public void getConfig() {
		// PREPARE
		User user = new User("test","test");
		storageMock.addUser(user);
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		formConfig.setOwner(user.getId());
		user.addPermission(storageMock, FormConfigPermission.onInstance(Ability.READ, formConfig.getId()));
		configs.put(formConfig.getId(),formConfig);
		
		// EXECUTE
		 FormConfigFullRepresentation response = processor.getConfig(new DatasetId("testDataset"), user, formConfig.getId());
		
		// CHECK
		assertThat(response).isEqualTo(FormConfigFullRepresentation.builder()
			.formType(form.getClass().getAnnotation(CPSType.class).id())
			.id(formConfig.getId())
			.label(formConfig.getLabel())
			.own(true)
			.ownerName(user.getLabel())
			.shared(false)
			.system(false)
			.tags(formConfig.getTags())
			.values(values)
			.build());
		
	}
	
	@Test
	public void getConfigs() {
		// PREPARE
		User user = new User("test","test");
		storageMock.addUser(user);
		
		ExportForm form2 = new ExportForm();
		RelativeMode mode3 = new RelativeMode();
		form2.setTimeMode(mode3);
		mode3.setForm(form);
		mode3.setFeatures(List.of(new CQConcept()));
		mode3.setOutcomes(List.of(new CQConcept()));
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		JsonNode values2 = mapper.valueToTree(form2);
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		FormConfig formConfig2 = new FormConfig(form2.getClass().getAnnotation(CPSType.class).id(), values2);
		formConfig.setOwner(user.getId());
		formConfig2.setOwner(user.getId());
		user.addPermission(storageMock, FormConfigPermission.onInstance(Ability.READ, formConfig.getId()));
		user.addPermission(storageMock, FormConfigPermission.onInstance(Ability.READ, formConfig2.getId()));
		configs.put(formConfig.getId(),formConfig);
		configs.put(formConfig2.getId(),formConfig2);
		
		// EXECUTE
		 Stream<FormConfigOverviewRepresentation> response = processor.getConfigsByFormType(user, Optional.empty());
		
		// CHECK
		assertThat(response).containsExactlyInAnyOrder(
			FormConfigOverviewRepresentation.builder()
				.formType(form.getClass().getAnnotation(CPSType.class).id())
				.id(formConfig.getId())
				.label(formConfig.getLabel())
				.own(true)
				.ownerName(user.getLabel())
				.shared(false)
				.system(false)
				.tags(formConfig.getTags())
				.build(),
			FormConfigOverviewRepresentation.builder()
				.formType(form2.getClass().getAnnotation(CPSType.class).id())
				.id(formConfig2.getId())
				.label(formConfig2.getLabel())
				.own(true)
				.ownerName(user.getLabel())
				.shared(false)
				.system(false)
				.tags(formConfig2.getTags())
				.build());
	}
	
	@Test
	public void patchConfig() {
		// PREPARE
		User user = new User("test","test");
		storageMock.addUser(user);
		Group group1 = new Group("test1","test1");
		storageMock.addGroup(group1);
		Group group2 = new Group("test2","test2");
		storageMock.addGroup(group2);
		
		group1.addMember(storageMock, user);
		group2.addMember(storageMock, user);
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		formConfig.setOwner(user.getId());
		user.addPermission(storageMock, FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, formConfig.getId()));
		configs.put(formConfig.getId(),formConfig);
		
		// EXECUTE PART 1
		processor.patchConfig(
			 user,
			 new DatasetId("testDataset"),
			 formConfig.getId(), 
			 MetaDataPatch.builder()
				 .label("newTestLabel")
				 .tags(new String[] {"tag1", "tag2"})
				 .shared(true)
				 .groups(List.of(group1.getId()))
			 	.build()
			 );
		
		// CHECK PART 1
		FormConfig patchedFormExpected = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		patchedFormExpected.setLabel("newTestLabel");
		patchedFormExpected.setShared(true);
		patchedFormExpected.setTags(new String[] {"tag1", "tag2"});
		
		assertThat(storageMock.getFormConfig(formConfig.getId())).isEqualToComparingOnlyGivenFields(patchedFormExpected, "formType", "label","shared","tags");

		assertThat(groups.get(group1.getId()).getPermissions()).contains(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_SHAREHOLDER, formConfig.getId()));
		assertThat(groups.get(group2.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_SHAREHOLDER, formConfig.getId()));
		
		
		
		// EXECUTE PART 2 (Unshare)
		processor.patchConfig(
			 user,
			 new DatasetId("testDataset"),
			 formConfig.getId(), 
			 MetaDataPatch.builder()
				 .shared(false)
				 .groups(List.of(group1.getId(), group2.getId()))
			 	.build()
			 );
		
		// CHECK PART 2
		patchedFormExpected.setShared(false);
		
		assertThat(storageMock.getFormConfig(formConfig.getId())).isEqualToComparingOnlyGivenFields(patchedFormExpected, "formType","label","shared","tags");

		assertThat(groups.get(group1.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_SHAREHOLDER, formConfig.getId()));
		assertThat(groups.get(group2.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_SHAREHOLDER, formConfig.getId()));
	}

}
