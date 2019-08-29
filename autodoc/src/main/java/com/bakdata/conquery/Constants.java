package com.bakdata.conquery;

import java.util.Set;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.MetaIdRef;
import com.bakdata.conquery.io.jackson.serializer.MetaIdRefCollection;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.model.Base;
import com.bakdata.conquery.model.Group;
import com.bakdata.conquery.models.common.KeyValue;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.ValidityDate;
import com.bakdata.conquery.models.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.preproc.outputs.Output;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.concept.specific.CQExternalResolved;
import com.bakdata.conquery.util.Doc;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Constants {
	public static final Group[] GROUPS = {
		Group.builder().name("Concept JSONs")
			.description("Each `*.concept.json` has to contain exactly one [Concept](#Concept).")
			.base(new Base(Concept.class, "A concept is collections of filters and selects and their connection to tables."))
			.base(new Base(CTCondition.class, "These represent guard conditions. A value matches a [ConceptElement](#ConceptElement) if it matches its condition and its parent"))
			.base(new Base(Filter.class, "These are used to define filters, than can be used to reduce the result set."))
			.base(new Base(Select.class, "These are used to define selects, that can be used to create additional CSV columns."))
			.otherClass(Connector.class)
			.otherClass(KeyValue.class)
			.otherClass(ConceptTreeChild.class)
			.otherClass(FilterTemplate.class)
			.otherClass(ValidityDate.class)
			.hide(UniversalSelect.class)
			.build(),
		Group.builder().name("Import JSONs")
			.description("Each `*.import.json` has to contain exactly one [ImportDescriptor](#ImportDescriptor).")
			.base(new Base(Output.class, ""))
			.hide(UniversalSelect.class)
			.build()/*,
		Group.builder().name("API JSONs")
			.base(new Base(IQuery.class, ""))
			.base(new Base(CQElement.class, ""))
			.base(new Base(FilterValue.class, ""))
			.hide(CQExternalResolved.class)
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
