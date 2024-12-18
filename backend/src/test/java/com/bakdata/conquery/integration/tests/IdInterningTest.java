package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IdInterningTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		final InternalMapperFactory mapperFactory = new InternalMapperFactory(conquery.getConfig(), conquery.getValidator());


		{
			final ObjectMapper mapper = Jackson.MAPPER.copy();

			mapperFactory.customizeApiObjectMapper(mapper, conquery.getDatasetRegistry(), conquery.getMetaStorage());
			conquery.getDataset().injectInto(mapper);

			final ObjectReader objectReader = mapper.readerFor(ConceptTreeChildId.class);

			final String raw = "\"1.concepts.2.3.4\"";

			final ConceptTreeChildId id1 = objectReader.readValue(raw);
			final ConceptTreeChildId id2 = objectReader.readValue(raw);

			assertThat(id1).isSameAs(id2);
			assertThat(id1.getParent()).isSameAs(id2.getParent());
			assertThat(id1.findConcept()).isSameAs(id2.findConcept());
			assertThat(id1.findConcept().getDataset()).isSameAs(id2.findConcept().getDataset());
		}

		{
			ObjectMapper mapper = mapperFactory.createManagerPersistenceMapper(conquery.getDatasetRegistry(), conquery.getMetaStorage());

			final ConceptTreeChildId id = new ConceptTreeChildId(
					new ConceptTreeChildId(
							new ConceptId(
									conquery.getDataset().getId(),
									"2"
							),
							"3"
					),
					"4"
			);

			final ConceptTreeChildId copy = mapper.readValue(mapper.writeValueAsBytes(id), ConceptTreeChildId.class);

			assertThat(copy).isEqualTo(id);
			assertThat(copy).hasSameHashCodeAs(id);
			assertThat(copy.toString()).isEqualTo(id.toString());
		}

	}
}