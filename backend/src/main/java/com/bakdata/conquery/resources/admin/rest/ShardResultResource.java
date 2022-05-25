package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.io.result.arrow.ArrowUtil;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.util.ResourceUtil;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

@Path("shard-results")
public class ShardResultResource {

	private BufferAllocator bufferAllocator;

	@POST
	@Path("{" + QUERY + "}")
	@Consumes(AdditionalMediaTypes.ARROW_STREAM)
	public <E extends ManagedExecution<?> & SingleTableResult> void addResult(@NotNull InputStream importStream, @PathParam(ResourceConstants.QUERY) E exec) throws IOException {

		final List<ArrowType> expectedTypes = ArrowUtil.extractTypes(exec.getSchema(null));

		try(ArrowStreamReader reader = new ArrowStreamReader(importStream, bufferAllocator)) {
			final VectorSchemaRoot vectorSchemaRoot = reader.getVectorSchemaRoot();
			List<ArrowType> recievedTypes = ArrowUtil.extractTypes(vectorSchemaRoot.getSchema());

			checkTypesEqual(expectedTypes, recievedTypes);

			final VectorUnloader vectorUnloader = new VectorUnloader(vectorSchemaRoot);

			while(reader.loadNextBatch()) {
				exec.addShardBatch(vectorUnloader.getRecordBatch());
			}

		}
	}

	private static void checkTypesEqual(List<ArrowType> t0, List<ArrowType> t1){
		if (t0.size() != t1.size()) {
			throw new IllegalArgumentException("The schemas differ in length");
		}
		for (int i = 0; i < t0.size(); i++) {
			if (t0.get(i).equals(t1.get(i))) {
				throw new IllegalArgumentException("The position of types differs");
			}
		}
	}
}
