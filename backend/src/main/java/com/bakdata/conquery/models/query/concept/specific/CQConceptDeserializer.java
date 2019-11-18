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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deserializer for {@link CQConcept}. Specifies the actual query element based on the Concept it is targeting.
 * This can be used to write plug-ins transparently, injecting concept-dependent behaviour at json-parse-time.
 *
 * We create a {@link Map} from {@link Concept} to {@link CQConcept} based on the {@link CQConceptSpecifier} annotation.
 */
@Slf4j
public class CQConceptDeserializer extends JsonDeserializer<CQConcept> {

	/**
	 * Map from {@link Concept} to specify the {@link CQConcept} class to deserialize.
	 */
	private static final Map<Class<? extends Concept<?>>, Class<? extends CQConcept>> transformers = new HashMap<>();

	static {
		try {
			// Search for all classes and their annotations.
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

			// We are only interested in classes with {@link CQConceptSpecifier} annotations.
			final List<Class<CQConcept>> specifiers = scan.getClassesWithAnnotation(CQConceptSpecifier.class.getName())
														  .loadClasses(CQConcept.class);

			// Store the mapping from Concept to CQConcept class.
			for (Class<CQConcept> specifierClass : specifiers) {
				final Class<? extends Concept<?>> concept = specifierClass.getAnnotation(CQConceptSpecifier.class).concept();
				transformers.put(concept, specifierClass);
			}

		} catch (Exception e) {
			log.error("Error while loading CQConceptSpecifiers.", e);
		}
	}

	@Override
	public Class<?> handledType() {
		return CQConcept.class;
	}

	/**
	 * Specify class to be deserialized as based on {@link CQConcept::ids} value.
	 **/
	@Override
	public CQConcept deserialize(JsonParser parser, DeserializationContext ctxt)
			throws IOException, JsonMappingException {



		// Read tree starting at token.
		final ObjectCodec codec = parser.getCodec();
		final ObjectNode treeNode = codec.readValue(parser, ObjectNode.class);

		if(transformers.isEmpty()) {
			return deserializeAs(treeNode.traverse(codec), ctxt, CQConcept.class);
		}

		// Try to read id's field as that contains the information specifying the targeted concept.
		final ConceptElementId<?>[] elements = treeNode.get(CQConcept.Fields.ids).traverse(codec).readValueAs(ConceptElementId[].class);

		if (elements == null || elements.length == 0) {
			return deserializeAs(treeNode.traverse(codec), ctxt, CQConcept.class);
		}

		final ConceptElementId<?> first = elements[0];

		// we are only interested in the Concept, which is the same over all ids.
		final ConceptElement[] conceptElements = CQConcept.resolveConcepts(
				Collections.singletonList(first),
				Objects.requireNonNull(CentralRegistry.getForDataset(ctxt, first.getDataset()), () -> String.format("Unable to find Central registry for dataset `%s`", first.getDataset()))
		);

		if (conceptElements.length == 0) {
			return deserializeAs(treeNode.traverse(codec), ctxt, CQConcept.class);
		}

		// Read concept of first ConceptElement and resolve to specific CQConcept sub-class if available.
		final Class<? extends CQConcept> clazz = transformers.getOrDefault(conceptElements[0].getConcept().getClass(), CQConcept.class);

		// Deserialize with specific class.
		return deserializeAs(treeNode.traverse(codec), ctxt, clazz);
	}

	/**
	 * Manually deserialize {@link CQConcept} or subtyes from specified class.
	 * We are the registered deserializer for the class, so we cannot just delegate further, but want to leverage as much Jackson capacities as possible.
	 */
	private static CQConcept deserializeAs(JsonParser parser, DeserializationContext ctxt, Class<? extends CQConcept> clazz) throws IOException {
		// Construct new serializer and corresponding deserializer
		final JavaType type = ctxt.constructType(clazz);
		final JsonDeserializer<Object> beanDeserializer = ctxt.getFactory()
															  .createBeanDeserializer(ctxt, type, ctxt.getConfig().introspect(type));

		// Set-up deserializer for current context etc.
		((SuperSonicBeanDeserializer) beanDeserializer).resolve(ctxt);

		// parsers created with TreeNode need to be stepped once to be used in the rest of Jackson.
		parser.nextToken();
		return (CQConcept) beanDeserializer.deserialize(parser, ctxt);
	}
}