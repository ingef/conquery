package com.bakdata.eva.models;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.util.support.TestAuth;
import com.bakdata.eva.forms.auform.AUForm;
import com.bakdata.eva.forms.common.Form;
import com.bakdata.eva.forms.common.Matched;
import com.bakdata.eva.forms.common.TimeAccessedResult;
import com.bakdata.eva.forms.common.TimeSelector;
import com.bakdata.eva.forms.common.UserInfo;
import com.bakdata.eva.forms.map.MapForm;
import com.bakdata.eva.forms.map.MapForm.Granularity;
import com.bakdata.eva.forms.map.MapForm.Region;
import com.bakdata.eva.forms.psm.PSMForm;
import com.bakdata.eva.forms.queries.FormQuery;
import com.bakdata.eva.models.forms.DateContext;
import com.bakdata.eva.models.forms.DateContextMode;
import com.bakdata.eva.models.forms.EventIndex;
import com.bakdata.eva.models.forms.Resolution;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;

public class SerializationTests {

	@Test
	public void testFormQuery() throws IOException, JSONException {
		FormQuery query = new FormQuery(
			Int2ObjectMaps
				.singleton(0, DateContext.generateAbsoluteContexts(CDateRange.exactly(LocalDate.now()), DateContextMode.COMPLETE_ONLY)));

		CQConcept concept = new CQConcept();
		ConceptId conceptId = new ConceptId(new DatasetId("testDataset"), "testConcept");
		ConceptElementId<?> ids[] = { conceptId };
		concept.setIds(Arrays.asList(ids));
		CQTable tables[] = { new CQTable() };
		tables[0].setId(new ConnectorId(conceptId, "testConnector"));
		tables[0].setConcept(concept);
		concept.setTables(Arrays.asList(tables));
		query.setRoot(concept);
		SerializationTestUtil.forType(FormQuery.class).test(query);
	}

	@Test
	public void testPSMForm() throws JSONException, IOException {
		PSMForm form = new PSMForm();

		// lazy, reuse everything
		TimeAccessedResult group = new TimeAccessedResult();
		group.setTimestamp(TimeSelector.FIRST);
		group.setId(new ManagedExecutionId(new DatasetId("testdataset"), UUID.randomUUID()));
		form.setControlGroup(group);
		form.setIndexDate(EventIndex.NEUTRAL);
		form.setFeatureGroup(group);
		form.setTimeUnit(Resolution.DAYS);

		CQConcept concept = new CQConcept();
		ConceptId conceptId = new ConceptId(new DatasetId("testDataset"), "testConcept");
		ConceptElementId<?> ids[] = { conceptId };
		concept.setIds(Arrays.asList(ids));
		CQTable tables[] = { new CQTable() };
		tables[0].setId(new ConnectorId(conceptId, "testConnector"));
		tables[0].setConcept(concept);
		concept.setTables(Arrays.asList(tables));
		Matched matched = new Matched();
		matched.setChildren(Arrays.asList(concept));
		form.setFeatures(Arrays.asList(matched));
		form.setOutcomes(Arrays.asList(matched));
		form.setUser(new UserInfo(TestAuth.TestUser.INSTANCE));

		SerializationTestUtil.forType(Form.class).test(form);
	}

	@Test
	public void testAUForm() throws JSONException, IOException {
		AUForm form = new AUForm();

		// lazy, reuse everything
		TimeAccessedResult group = new TimeAccessedResult();
		group.setTimestamp(TimeSelector.FIRST);
		group.setId(new ManagedExecutionId(new DatasetId("testdataset"), UUID.randomUUID()));

		form.setUser(new UserInfo(TestAuth.TestUser.INSTANCE));
		form.setQueryGroup(new ManagedExecutionId(new DatasetId("testdataset"), UUID.randomUUID()));
		form.setDateRange(new Range<LocalDate>(LocalDate.now(), LocalDate.now()));

		SerializationTestUtil.forType(Form.class).test(form);
	}

	@Test
	public void testMapForm() throws JSONException, IOException {
		MapForm form = new MapForm();

		form.setGranularity(Granularity.FEDERAL_STATE);
		form.setQueryGroup(new ManagedExecutionId(new DatasetId("testdataset"), UUID.randomUUID()));
		form.setDateRange(new Range<LocalDate>(LocalDate.now(), LocalDate.now()));
		form.setResolution(DateContextMode.COMPLETE_ONLY);
		form.setRegion(Region.BREMEN);
		form.setUser(new UserInfo(TestAuth.TestUser.INSTANCE));

		SerializationTestUtil.forType(Form.class).test(form);
	}
}
