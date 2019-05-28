package com.bakdata.conquery.models.events.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.specific.StringParser;

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
					Object[] event = new Object[17];
					arrays.add(event);

					if(r.nextBoolean()) {
						event[0] = (CDate.ofLocalDate(LocalDate.now()));
					}
					if(r.nextBoolean()) {
						event[1] = Long.toHexString(r.nextLong());
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
					if(r.nextBoolean()) {
						event[16] = BigDecimal.valueOf(r.nextInt(4), r.nextInt(120)-60);
					}
				}
				arrays.trimToSize();

				return Arguments.of(numberOfValues, arrays);
			}
			);
	}

	public Block generateBlock(List<Object[]> arrays) throws IOException {
		Parser[] parser = new Parser[] {
			MajorTypeId.DATE.createParser(),
			MajorTypeId.STRING.createParser(),
			MajorTypeId.STRING.createParser(),
			MajorTypeId.STRING.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.STRING.createParser(),
			MajorTypeId.STRING.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.STRING.createParser(),
			MajorTypeId.DECIMAL.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.DECIMAL.createParser()
		};


		for(Object[] event:arrays) {
			for(int i=0;i<parser.length;i++) {
				try {
					//only parse strings, this test otherwise already creates parsed values
					if(parser[i] instanceof StringParser && event[i] != null) {
						event[i] = parser[i].parse((String)event[i]);
					}
					parser[i].addLine(event[i]);
				} catch(Exception e) {
					throw new IllegalArgumentException("Column "+i, e);
				}
			}
		}
		
		Import imp = new Import();
		imp.setTable(new TableId(new DatasetId("test_dataset"), "table"));
		imp.setName("import");
		imp.setColumns(IntStream.range(0, parser.length)
			.mapToObj(i->column(imp,i))
			.toArray(ImportColumn[]::new)
		);

		Decision[] decisions = Arrays.stream(parser).map(Parser::findBestType).toArray(Decision[]::new);
		for(int i=0;i<parser.length;i++) {
			imp.getColumns()[i].setType(decisions[i].getType());
			log.info("{}: {} mapped to {}", imp.getColumns()[i], parser[i], imp.getColumns()[i].getType());
		}
		
		List<Object[]> result = new ArrayList<>(arrays.size());
		for(Object[] event:arrays) {
			Object[] line = Arrays.copyOf(event, event.length);
			for(int i=0;i<imp.getColumns().length;i++) {
				if(event[i] != null) {
					line[i] = decisions[i].getTransformer().transform(event[i]);
				}
			}
			result.add(line);
		}
		for(int i=0;i<imp.getColumns().length;i++) {
			imp.getColumns()[i].getType().writeHeader(new NullOutputStream());
		}

		return imp.getBlockFactory().createBlock(
			0,
			imp,
			result
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
				String message = "checking "+c+" "+block.getImp().getColumns()[c].getType()+":"+i+" = "+orig;
				
				if(orig == null) {
					assertThat(block.has(i, fake))
						.as(message)
						.isFalse();
				}
				else {
					assertThat(block.has(i, fake))
						.as(message+" is not null")
						.isTrue();
					
					if(orig instanceof BigDecimal) {
						assertThat((BigDecimal)block.getAsObject(i, fake))
							.as(message)
							.usingComparator(BigDecimal::compareTo)
							.isEqualTo(orig);
					}
					else {
						assertThat(block.getAsObject(i, fake))
							.as(message)
							.isEqualTo(orig);
					}
				}
				
			}
			block.calculateMap(i, block.getImp());
		}
		CentralRegistry registry = new CentralRegistry();
		registry.register(block.getImp());

		SerializationTestUtil.testSerialization(block, Block.class, registry);
	}

	private ImportColumn column(Import imp, int pos) {
		ImportColumn col = new ImportColumn();
		col.setName(String.format("@column%02d", pos));
		col.setParent(imp);
		col.setPosition(pos);
		return col;
	}
}
