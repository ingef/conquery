package com.bakdata.conquery.models.query.concept.specific;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.afterburner.deser.SuperSonicBeanDeserializer;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CQConceptDeserializer extends JsonDeserializer<CQConcept> {

	private static Map<Class<? extends Concept<?>>, Class<? extends CQConcept>> transformers = new HashMap<>();

	private static void initTransformers() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		final ScanResult scan = new ClassGraph()
										.enableClassInfo()
										.enableAnnotationInfo()
										//blacklist some packages that contain large libraries
										.blacklistPackages(
												"groovy",
												"org.codehaus.groovy",
												"org.apache",
												"org.eclipse",
												"com.google"
										)
										.scan();

		final List<Class<CQConcept>> specifiers = scan.getClassesWithAnnotation(CQConceptSpecifier.class.getName()).loadClasses(CQConcept.class);

		for (Class<CQConcept> specifierClass : specifiers) {
			final Class<? extends Concept<?>> concept = specifierClass.getAnnotation(CQConceptSpecifier.class).concept();
			transformers.put(concept, specifierClass);
		}

	}

	static {
		try {
			initTransformers();
		} catch (Exception e) {
			log.error("Error while loading transformers.", e);
		}
	}

	@Override
	public Class<?> handledType() {
		return CQConcept.class;
	}

	@Override
	public CQConcept deserialize(JsonParser parser, DeserializationContext ctxt)
			throws IOException, JsonMappingException {

		final ObjectCodec codec = parser.getCodec();
		final ObjectNode treeNode = codec.readValue(parser, ObjectNode.class);

		final ConceptElementId<?>[] elements = treeNode.get(CQConcept.Fields.ids).traverse(codec).readValueAs(ConceptElementId[].class);

		if (elements == null || elements.length == 0) {
			return deserializeAs(treeNode.traverse(codec), ctxt, CQConcept.class);
		}

		// we are only interested in the Concept, which is the same over all ids.
		final ConceptElement[] conceptElements = CQConcept.resolveConcepts(Collections.singletonList(elements[0]), CentralRegistry.getForDataset(ctxt, elements[0].getDataset()));

		//TODO This might even be an error.
		if (conceptElements.length == 0) {
			return deserializeAs(treeNode.traverse(codec), ctxt, CQConcept.class);
		}

		final Class<? extends CQConcept> clazz = transformers.getOrDefault(conceptElements[0].getConcept().getClass(), CQConcept.class);

		return deserializeAs(treeNode.traverse(codec), ctxt, clazz);
	}

	private CQConcept deserializeAs(JsonParser parser, DeserializationContext ctxt, Class<? extends CQConcept> clazz) throws IOException {
		parser.nextToken();
		final JavaType type = ctxt.constructType(clazz);

		final JsonDeserializer<Object> beanDeserializer = ctxt.getFactory()
															  .createBeanDeserializer(ctxt, type, ctxt.getConfig().introspect(type));

		((SuperSonicBeanDeserializer) beanDeserializer).resolve(ctxt);

		return (CQConcept) beanDeserializer.deserialize(parser, ctxt);
	}
}