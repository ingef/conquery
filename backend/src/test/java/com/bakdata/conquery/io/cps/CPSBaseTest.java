package com.bakdata.conquery.io.cps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class CPSBaseTest {

	@TestFactory
	public Stream<DynamicTest> testCPSBase() {
		return CPSTypeIdResolver
			.listImplementations()
			.stream()
			.map(this::createTest)
			.sorted(Comparator.comparing(DynamicTest::getDisplayName));
	}
	
	public DynamicTest createTest(Pair<Class<?>,Class<?>> baseTypePair) {
		String name;
		if(baseTypePair.getRight().getAnnotation(CPSType.class)!=null) {
			name = baseTypePair.getRight().getAnnotation(CPSType.class).id();
		}
		else {
			name = "multiple";
		}
				
		name += " -> "+baseTypePair.getRight().getSimpleName();
		return DynamicTest.dynamicTest(name, ()->test(baseTypePair.getLeft(), baseTypePair.getRight()));
	}

	public void test(Class<?> base, Class<?> type) {
		CPSType[] annos = type.getAnnotationsByType(CPSType.class);
		assertThat(annos).isNotEmpty();
		for(CPSType anno : annos) {
			assertThat(anno.base()).isNotNull();
			assertThat(anno.id()).isNotEmpty();
			assertThat(anno.base()).isEqualTo(base);
			assertThat(base).hasAnnotation(CPSBase.class);
			assertThat(base).isAssignableFrom(base);
			if(anno.subTyped()) {				
				assertThat(SubTyped.class).isAssignableFrom(type);
			}
		}
	}
}
