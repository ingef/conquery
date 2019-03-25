package com.bakdata.conquery.models.events.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.specific.StringType;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class GenerationTests {
	public static Stream<Arguments> createRandomContent() {
		return IntStream
			.of(100)
			.mapToObj( numberOfValues -> {
				Random r = new Random(7);
				ArrayList<Object[]> arrays = new ArrayList<>();
				for(int i = 0;i<numberOfValues;i++) {
					Object[] event = new Object[16];
					arrays.add(event);

					if(r.nextBoolean()) {
						event[0] = (CDate.ofLocalDate(LocalDate.now()));
					}
					if(r.nextBoolean()) {
						event[1] = Integer.toHexString(r.nextInt());
					}
					if(r.nextBoolean()) {
						event[2] = Integer.valueOf(r.nextInt()).toString();
					}
					if(r.nextBoolean()) {
						event[3] = Integer.valueOf(r.nextInt()).toString();
					}
					if(r.nextBoolean()) {
						event[4] = Long.valueOf(r.nextInt());
					}
					if(r.nextBoolean()) {
						event[5] = Integer.valueOf((byte)r.nextInt()).toString();
					}
					if(r.nextBoolean()) {
						event[6] = Integer.valueOf(r.nextInt()).toString();
					}
					if(r.nextBoolean()) {
						event[7] = Long.valueOf((byte)r.nextInt());
					}
					if(r.nextBoolean()) {
						event[8] = Long.valueOf((byte)r.nextInt());
					}
					if(r.nextBoolean()) {
						event[9] = Integer.valueOf(r.nextInt()).toString();
					}
					if(r.nextBoolean()) {
						event[10] = BigDecimal.valueOf(r.nextInt(4), r.nextInt(10)-5);
					}
					if(r.nextBoolean()) {
						event[11] = Long.valueOf((byte)r.nextInt());
					}
					event[12] = Long.valueOf(r.nextInt());
					event[13] = Long.valueOf(r.nextInt());
					//event[14] = null;
					event[15] = Long.valueOf(r.nextInt());
				}
				arrays.trimToSize();

				return Arguments.of(numberOfValues, arrays);
			}
			);
	}

	public Block generateBlock(List<Object[]> arrays) {
		Import imp = new Import();
		imp.setTable(new TableId(new DatasetId("test_dataset"), "table"));
		imp.setName("import");
		imp.setColumns(new ImportColumn[] {
				column(imp, 0, MajorTypeId.DATE.createType()),
				column(imp, 1, MajorTypeId.STRING.createType()),
				column(imp, 2, MajorTypeId.STRING.createType()),
				column(imp, 3, MajorTypeId.STRING.createType()),
				column(imp, 4, MajorTypeId.INTEGER.createType()),
				column(imp, 5, MajorTypeId.STRING.createType()),
				column(imp, 6, MajorTypeId.STRING.createType()),
				column(imp, 7, MajorTypeId.INTEGER.createType()),
				column(imp, 8, MajorTypeId.INTEGER.createType()),
				column(imp, 9, MajorTypeId.STRING.createType()),
				column(imp, 10, MajorTypeId.DECIMAL.createType()),
				column(imp, 11, MajorTypeId.INTEGER.createType()),
				column(imp, 12, MajorTypeId.INTEGER.createType()),
				column(imp, 13, MajorTypeId.INTEGER.createType()),
				column(imp, 14, MajorTypeId.INTEGER.createType()),
				column(imp, 15, MajorTypeId.INTEGER.createType())
		});


		for(Object[] event:arrays) {
			for(int i=0;i<imp.getColumns().length;i++) {
				try {
					imp.getColumns()[i].getType().addLine(event[i]);

					if(imp.getColumns()[i].getType() instanceof StringType && event[i] != null) {
						event[i] = imp.getColumns()[i].getType().parse((String)event[i]);
					}

				} catch(Exception e) {
					throw new IllegalArgumentException("Column "+i, e);
				}
			}
		}

		CType[] originalTypes = new CType[imp.getColumns().length];
		for(int i=0;i<imp.getColumns().length;i++) {
			originalTypes[i] = imp.getColumns()[i].getType();
			imp.getColumns()[i].setType(imp.getColumns()[i].getType().bestSubType());
			log.info("{}: {} mapped to {}", imp.getColumns()[i], originalTypes[i], imp.getColumns()[i].getType());
		}

		return imp.getBlockFactory().createBlock(
			0,
			imp,
			arrays
				.stream()
				.map(e -> {
					Object[] transformed = new Object[e.length];
					for(int i=0;i<transformed.length;i++) {
						if(e[i] == null) {
							transformed[i] = null;
						}
						else {
							transformed[i] = imp.getColumns()[i].getType().transformFromMajorType(originalTypes[i], e[i]);
						}
					}
					return transformed;
				})
				.collect(Collectors.toList())
		);
	}

	@ParameterizedTest(name="{0}")
	@MethodSource("createRandomContent")
	public void testSerialization(int numberOfValues, List<Object[]> arrays) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, NoSuchMethodException, SecurityException, JSONException {
		Block block = generateBlock(arrays);
		for(int i=0;i<arrays.size();i++) {
			for(int c=0;c<arrays.get(i).length;c++) {
				Column fake = new Column();
				fake.setPosition(c);
				
				Object orig = arrays.get(i)[c];
				if(orig == null) {
					assertThat(block.has(i, fake))
						.as("checking "+c+":"+i+" = null")
						.isFalse();
				}
				else if(orig instanceof BigDecimal) {
					assertThat((BigDecimal)block.getAsObject(i, fake))
						.as("checking "+c+":"+i+" = '"+orig+"'")
						.usingComparator(BigDecimal::compareTo)
						.isEqualTo(orig);
				}
				else {
					assertThat(block.getAsObject(i, fake))
						.as("checking "+c+":"+i+" = '"+orig+"'")
						.isEqualTo(orig);
				}
			}
		}
		CentralRegistry registry = new CentralRegistry();
		registry.register(block.getImp());

		SerializationTestUtil.testSerialization(block, Block.class, registry);

		for(ImportColumn col : block.getImp().getColumns()) {
			SerializationTestUtil.testSerialization(col.getType(), CType.class, Dictionary.class);
		}
	}

	private ImportColumn column(Import imp, int pos, CType<?, ?> valueType) {
		ImportColumn col = new ImportColumn();
		col.setName(String.format("@column%02d", pos));
		col.setParent(imp);
		col.setPosition(pos);
		col.setType(valueType);
		return col;
	}
}
