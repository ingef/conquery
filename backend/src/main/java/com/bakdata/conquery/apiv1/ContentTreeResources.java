package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.apiv1.ResourceConstants.CONCEPT;
import static com.bakdata.conquery.apiv1.ResourceConstants.DATASET;
import static com.bakdata.conquery.apiv1.ResourceConstants.FILTER;
import static com.bakdata.conquery.apiv1.ResourceConstants.TABLE;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import com.bakdata.conquery.apiv1.ContentTreeProcessor.ResolvedConceptsResult;
import com.bakdata.conquery.models.api.description.FENode;
import com.bakdata.conquery.models.api.description.FERoot;
import com.bakdata.conquery.models.api.description.FEValue;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.ResourceUtil;

import io.dropwizard.auth.Auth;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("datasets")
@Produces(AdditionalMediaTypes.JSON)
@PermitAll
public class ContentTreeResources {

	private final ContentTreeProcessor processor;
	private final ResourceUtil dsUtil;

	public ContentTreeResources(Namespaces namespaces) {
		this.processor = new ContentTreeProcessor(namespaces);
		this.dsUtil = new ResourceUtil(namespaces);
	}

	@GET
	public List<IdLabel> getDatasets(@Auth User user) {
		return processor.getDatasets(user);
	}

	@GET
	@Path("{" + DATASET + "}/concepts")
	public FERoot getRoot(@Auth User user, @PathParam(DATASET) DatasetId datasetId) {
		authorize(user, datasetId, Ability.READ);
		return processor.getRoot(dsUtil.getStorage(datasetId));
	}

	@GET
	@Path("{" + DATASET + "}/concepts/{" + CONCEPT + "}")
	public Response getNode(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(CONCEPT) ConceptId id, @Context HttpServletRequest req) {
		authorize(user, datasetId, Ability.READ);
		// if (useCaching && req.getHeader(HttpHeaders.IF_NONE_MATCH) != null
		// &&
		// currentTag.equals(EntityTag.valueOf(req.getHeader(HttpHeaders.IF_NONE_MATCH))))
		// {
		// return Response.status(HttpServletResponse.SC_NOT_MODIFIED).build();
		// }
		Dataset dataset = dsUtil.getDataset(datasetId);
		Map<ConceptElementId<?>, FENode> result = processor.getNode(dataset, id);

		if (result == null) {
			throw new WebApplicationException("There is not concept with the id " + id + " in the dataset " + dataset, Status.NOT_FOUND);
		}
		else {
			return Response
				.ok(result)
				// .tag(currentTag)
				.build();
		}
	}

	@POST
	@Path("{" + DATASET + "}/concepts/{" + CONCEPT + "}/tables/{" + TABLE + "}/filters/{" + FILTER + "}/autocomplete")
	public List<FEValue> autocompleteTextFilter(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(CONCEPT) ConceptElementId conceptElementId, @PathParam(TABLE) TableId tableId, @PathParam(FILTER) FilterId filterId, @NotNull StringContainer text, @Context HttpServletRequest req) {

		authorize(user, datasetId, Ability.READ);

		if (text.getText().length() < 1) {
			throw new WebApplicationException("Too short text. Requires at least 1 characters.", Status.BAD_REQUEST);
		}

		Connector connector = dsUtil
			.getStorage(datasetId)
			.getConcept(conceptElementId.findConcept())
			.getConnectorByName(filterId.getConnector().getConnector());

		return processor
			.autocompleteTextFilter(dsUtil.getDataset(datasetId), connector.getTable(), connector.getFilter(filterId), text.getText());
	}

	@POST
	@Path("{" + DATASET + "}/concepts/{" + CONCEPT + "}/resolve")
	public ResolvedConceptsResult resolve(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(CONCEPT) ConceptElementId conceptElementId, @NotNull ConceptCodeList conceptCodes, @Context HttpServletRequest req) {
		authorize(user, datasetId, Ability.READ);
		ConceptElement<?> conceptElement = dsUtil
			.getStorage(datasetId)
			.getConcept(conceptElementId.findConcept())
			.getElementById(conceptElementId);

		List<String> codes = conceptCodes.getConcepts().stream().map(String::trim).collect(Collectors.toList());

		return processor.resolve(dsUtil.getDataset(datasetId), conceptElement, codes);
	}

	@POST
	@Path("{" + DATASET + "}/concepts/{" + CONCEPT + "}/tables/{" + TABLE + "}/filters/{" + FILTER + "}/resolve")
	public ResolvedConceptsResult resolveFilterValues(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @PathParam(CONCEPT) ConceptElementId conceptElementId, @PathParam(TABLE) TableId tableId, @PathParam(FILTER) FilterId filterId, FilterValues filterValues, @Context HttpServletRequest req) {
		authorize(user, datasetId, Ability.READ);

		Connector connector = dsUtil
			.getStorage(datasetId)
			.getConcept(conceptElementId.findConcept())
			.getConnectorByName(filterId.getConnector().getConnector());

		return processor
			.resolveFilterValues(
				dsUtil.getDataset(datasetId),
				connector.getTable(),
				connector.getFilter(filterId),
				filterValues.getValues());
	}

	@POST
	@Path("{" + DATASET + "}/concepts/search")
	public SearchResult search(@Auth User user, @PathParam(DATASET) DatasetId datasetId, @NotNull ConceptSearchParam conceptSearchParam, @Context HttpServletRequest req) {
		authorize(user, datasetId, Ability.READ);

		String query = conceptSearchParam.getQuery();

		if (StringUtils.isBlank(query)) {
			throw new WebApplicationException("Too short query.", Status.BAD_REQUEST);
		}

		int limit = conceptSearchParam.getLimit();
		return processor.search(dsUtil.getDataset(datasetId), query, limit);
	}

	@Getter
	@Setter
	public static class StringContainer {

		private String text;
	}

	@Getter
	@Setter
	public static class ConceptCodeList {

		private List<String> concepts;
	}

	@Getter
	@Setter
	public static class ConceptSearchParam {

		private String query;
		private int limit;
	}

	@Getter
	@Setter
	public static class FilterValues {

		private List<String> values;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	public static class SearchResult {

		private List<String> result;
		private int matches;
		private int limit;
		private int size;
	}
}
