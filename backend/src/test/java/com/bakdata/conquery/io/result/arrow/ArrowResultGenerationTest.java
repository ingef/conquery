package com.bakdata.conquery.io.result.arrow;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import com.bakdata.conquery.models.concepts.select.connector.specific.CountSelect;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowFileWriter;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.jupiter.api.Test;

@Slf4j
public class ArrowResultGenerationTest {

	@Test
	void test() {
		ManagedQuery mQuery = new ManagedQuery(null, null, null);

		List<EntityResult> results = mQuery.getResults();
		List<ColumnDescriptor> columnDes = mQuery.getColumnDescriptions();
		ResultInfoCollector infos = mQuery.collectResultInfos();

	}

	@Test
	void generateSchema() throws IOException {
		Schema schema = generateSchema(
			List.of(new SelectResultInfo(new CountSelect(), new CQConcept())),
			// Custom column namer so we don't require a dataset registry
			new PrintSettings(false, Locale.ROOT, null, (selectInfo, datasetRegistry) -> selectInfo.getSelect().getName()));

		assertThat(schema).isEqualTo(new Schema(List.of(new Field(null, FieldType.nullable(new ArrowType.Int(32, true)), null)), null));

		VectorSchemaRoot root = VectorSchemaRoot.create(schema, new RootAllocator());

		ArrowFileWriter writer = new ArrowFileWriter(root, new DictionaryProvider.MapDictionaryProvider(), new WritableByteChannel() {

			@Override
			public boolean isOpen() {
				return true;
			}

			@Override
			public void close() throws IOException {}

			@Override
			public int write(ByteBuffer src) throws IOException {
				int cnt = 0;
				while (src.hasRemaining()) {
					cnt++;
					src.get();
				}
				log.info("Wrote {} bytes", cnt);
				return cnt;
			}

		});

		List<EntityResult> results = List.of(new SinglelineContainedEntityResult(1, new Object[] { 1 }));

		writer.start();

		root.setRowCount(1);
		for (EntityResult result : results) {
			for (Object[] line : result.asContained().listResultLines()) {
				for (int i = 0; i < line.length; i++) {
					((IntVector) root.getVector(i)).set(0, (int) line[i]);
				}
			}
		}
		writer.writeBatch();

		writer.end();
		writer.close();
	}

	private final static Function<String, Field> NAMED_FIELD_DATE_DAY = (name) -> new Field(name,
		FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null);

	private static Schema generateSchema(@NonNull List<ResultInfo> infos, PrintSettings settings) {

		ImmutableList.Builder<Field> childrenBuilder = ImmutableList.builder();

		for (ResultInfo info : infos) {
			MajorTypeId internalType = info.getInternalType();
			switch (internalType) {
				case BOOLEAN:
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(ArrowType.Bool.INSTANCE), null));
					break;
				case DATE:
					childrenBuilder.add(NAMED_FIELD_DATE_DAY.apply(info.getUniqueName(settings)));
					break;
				case DATE_RANGE:
					childrenBuilder.add(
						new Field(info.getUniqueName(settings), FieldType.nullable(ArrowType.Struct.INSTANCE),
							ImmutableList.of(NAMED_FIELD_DATE_DAY.apply("begin"), NAMED_FIELD_DATE_DAY.apply("end"))));
					break;
				case DECIMAL:
					// Not sure at the moment how to determine the right scale and precision
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Decimal(0, 0)), null));
					break;
				case INTEGER:
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Int(32, true)), null));
					break;
				case MONEY:
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.Decimal(2, 0)), null));
					break;
				case REAL:
					childrenBuilder.add(
						new Field(info.getUniqueName(settings),
							FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE)), null));
					break;
				case STRING:
					childrenBuilder.add(new Field(info.getUniqueName(settings), FieldType.nullable(new ArrowType.LargeUtf8()), null));
					break;
				default:
					throw new IllegalStateException("Unknown column type " + internalType);
			}

		}

		return new Schema(childrenBuilder.build(), null);

	}

}
