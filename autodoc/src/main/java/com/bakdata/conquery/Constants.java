package com.bakdata.conquery;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.MetaIdRef;
import com.bakdata.conquery.io.jackson.serializer.MetaIdRefCollection;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.model.Base;
import com.bakdata.conquery.model.Group;
import com.bakdata.conquery.models.auth.AuthConfig;
import com.bakdata.conquery.models.common.KeyValue;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.ValidityDate;
import com.bakdata.conquery.models.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.config.APIConfig;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.ClusterConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.FrontendConfig;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.config.MinaConfig;
import com.bakdata.conquery.models.config.PluginConfig;
import com.bakdata.conquery.models.config.PreprocessingConfig;
import com.bakdata.conquery.models.config.PreprocessingDirectories;
import com.bakdata.conquery.models.config.QueryConfig;
import com.bakdata.conquery.models.config.StandaloneConfig;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.config.ThreadPoolDefinition;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.preproc.ImportDescriptor;
import com.bakdata.conquery.models.preproc.Input;
import com.bakdata.conquery.models.preproc.outputs.AutoOutput;
import com.bakdata.conquery.models.preproc.outputs.Output;
import com.bakdata.conquery.util.Doc;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.dropwizard.util.Duration;
import io.dropwizard.util.Size;

public class Constants {
	public static final Group[] GROUPS = {
		Group.builder().name("Concept JSONs")
			.description("Each `*.concept.json` has to contain exactly one [Concept](#Base-Concept).")
			.base(new Base(Concept.class, "A concept is a collection of filters and selects and their connection to tables."))
			.base(new Base(CTCondition.class, "These represent guard conditions. A value matches a [ConceptElement](#ConceptElement) if it matches its condition and its parent"))
			.base(new Base(Filter.class, "These are used to define filters, than can be used to reduce the result set."))
			.base(new Base(Select.class, "These are used to define selects, that can be used to create additional CSV columns."))
			.otherClass(Connector.class)
			.otherClass(KeyValue.class)
			.otherClass(ConceptTreeChild.class)
			.otherClass(FilterTemplate.class)
			.otherClass(ValidityDate.class)
			.markerInterface(UniversalSelect.class)
			.build(),
		Group.builder().name("Import JSONs")
			.description("Each `*.import.json` has to contain exactly one [ImportDescriptor](#Type-ImportDescriptor).")
			.base(new Base(Output.class, ""))
			.otherClass(ImportDescriptor.class)
			.otherClass(Input.class)
			.hide(AutoOutput.class)
			.build(),
		Group.builder().name("Config JSON")
			.description("The `config.json` is required for every type of execution. Its root element is a [ConqueryConfig](#Type-ConqueryConfig) object.")
			.base(new Base(AuthConfig.class, "An `AuthConfig` is used to define how users are authenticated."))
			.base(new Base(PluginConfig.class, "A `PluginConfig` is used to define settings for Conquery plugins."))
			.base(new Base(IdMappingConfig.class, "An `IdMappingConfig` is used to define how multi column IDs are printed and parsed"))
			.otherClass(APIConfig.class)
			.otherClass(ConqueryConfig.class)
			.otherClass(ClusterConfig.class)
			.otherClass(CSVConfig.class)
			.otherClass(FrontendConfig.class)
			.otherClass(LocaleConfig.class)
			.otherClass(PreprocessingConfig.class)
			.otherClass(QueryConfig.class)
			.otherClass(StandaloneConfig.class)
			.otherClass(StorageConfig.class)
			.otherClass(MinaConfig.class)
			.otherClass(FrontendConfig.CurrencyConfig.class)
			.otherClass(XodusConfig.class)
			.otherClass(ThreadPoolDefinition.class)
			.otherClass(PreprocessingDirectories.class)
			.hide(Charset.class)
			.hide(Currency.class)
			.hide(InetAddress.class)
			.hide(Locale.class)
			.hide(Duration.class)
			.hide(Size.class)
			.build()/*,
		Group.builder().name("API JSONs")
			.base(new Base(IQuery.class, ""))
			.base(new Base(CQElement.class, ""))
			.base(new Base(FilterValue.class, ""))
			.hide(CQExternalResolved.class)
			.build(),
		Group.builder().name("COnfig!")
			.build()*/
	};
	
	public static final String DOC = Doc.class.getName();
	public static final String JSON_CREATOR = JsonCreator.class.getName();
	public static final String CPS_TYPE = CPSType.class.getName();
	public static final Set<String> ID_REF = Set.of(NsIdRef.class.getName(), MetaIdRef.class.getName());
	public static final Set<String> ID_REF_COL = Set.of(NsIdRefCollection.class.getName(), MetaIdRefCollection.class.getName());
	public static final String JSON_IGNORE = JsonIgnore.class.getName();
	public static final String JSON_BACK_REFERENCE = JsonBackReference.class.getName();
	public static final String LIST_OF = "list of ";
	public static final String ID_OF = "ID of ";
}
