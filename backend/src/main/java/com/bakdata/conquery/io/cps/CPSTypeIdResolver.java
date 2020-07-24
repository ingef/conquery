package com.bakdata.conquery.io.cps;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Slf4j
public class CPSTypeIdResolver implements TypeIdResolver {

	public static final String ATTRIBUTE_SUB_TYPE = "subType";
	public static final String SEPARATOR_SUB_TYPE = "@";

	private static HashMap<Class<?>, CPSMap> globalMap;

	public static final ScanResult SCAN_RESULT;
	
	private JavaType baseType;
	private CPSMap cpsMap;

	@Override
	public void init(JavaType baseType) {
		this.baseType = baseType;
		this.cpsMap = new CPSMap();
		
		//this creates an aggregate map of all the children
		Iterable<Class<?>> types = Traverser.forGraph(
			(SuccessorsFunction<Class<?>>) node -> {
				Class<?> superclass = node.getSuperclass();
				List<Class<?>> interfaces = Arrays.asList(node.getInterfaces());
				return superclass == null 
					? interfaces
					: Iterables.concat(interfaces, Collections.singleton(superclass));
			}
		).breadthFirst(baseType.getRawClass());
		
		for(Class<?> type : types) {
			CPSMap local = globalMap.get(type);
			if(local != null) {
				cpsMap.merge(local);
			}
		}

		cpsMap.calculateInverse();
	}

	static {
		log.info("Scanning Classpath");
		//scan classpaths for annotated child classes

		SCAN_RESULT = new ClassGraph()
				.enableClassInfo()
				.enableAnnotationInfo().rejectPackages(
						"groovy",
						"org.codehaus.groovy",
						"org.apache",
						"org.eclipse",
						"com.google",
						"io",
						"com.auth0",
						"com.esotericsoftware",
						"org.glassfish"

				)
				.scan();
		
		log.info("Scanned: {} classes in classpath", SCAN_RESULT.getAllClasses().size());
		Set<Class<?>> types = new HashSet<>();
		types.addAll(SCAN_RESULT.getClassesWithAnnotation(CPSTypes.class.getName()).loadClasses());
		types.addAll(SCAN_RESULT.getClassesWithAnnotation(CPSType.class.getName()).loadClasses());
		
		globalMap = new HashMap<>();
		for(Class<?> type:types) {
			CPSType[] annos = type.getAnnotationsByType(CPSType.class);
			for(CPSType anno:annos) {
				CPSMap map = globalMap.computeIfAbsent(anno.base(), b->new CPSMap());
				
				//check if base is marked as base
				CPSBase baseAnno = anno.base().getAnnotation(CPSBase.class);
				if(baseAnno==null) {
					throw new IllegalStateException("The class "+anno.base()+" is used as a CPSBase in "+type+" but not annotated as such.");
				}
				if(!anno.base().isAssignableFrom(type)) {
					throw new IllegalStateException("The class "+anno.base()+" is used as a CPSBase in "+type+" but type is no subclass of it.");
				}
				if(anno.subTyped() && !SubTyped.class.isAssignableFrom(type)) {
					throw new IllegalStateException("The class "+type+" is flagged to support a subtyping information but does not implement "+ SubTyped.class.getName());
				}
				
				map.add(anno.id(), type);
			}
		}
		
		List<Class<?>> bases = SCAN_RESULT.getClassesWithAnnotation(CPSBase.class.getName()).loadClasses();
		for(Class<?> b:bases) {
			CPSMap map = globalMap.get(b);
			if(map==null) {
				log.warn("\tBase Class {}:\tNo registered types", b);
			}
			else {
				log.info("\tBase Class {}", b.getSimpleName());
				map.calculateInverse();
				for(Entry<Class<?>, String> e:map) {
					log.info("\t\t{}\t->\t{}", e.getValue(), e.getKey().getSimpleName());
				}
			}
			
		}
	}

	@Override
	public JavaType typeFromId(DatabindContext context, String id) {
		Class<?> result = cpsMap.getClassFromId(truncateSubTypeInformation(id));
		if(result == null) {
			throw new IllegalStateException("There is no type "+id+" for "+baseType.getTypeName()+". Try: "+getDescForKnownTypeIds());
		}
		String subTypeInfo = extractSubTypeInformation(id);
		if(!Strings.isNullOrEmpty(subTypeInfo)) {
			
			context.setAttribute(ATTRIBUTE_SUB_TYPE, subTypeInfo);
		}
		return TypeFactory.defaultInstance().constructSpecializedType(baseType, result);
	}
	
	public static String truncateSubTypeInformation(@NonNull String fullType) {
		int seperatorIndex = fullType.indexOf(SEPARATOR_SUB_TYPE);
		if(seperatorIndex < 0) {
			// Separator not found
			return fullType;
		}
		return fullType.substring(0, seperatorIndex);
	}
	
	public static String extractSubTypeInformation(@NonNull String fullType) {
		int seperatorIndex = fullType.indexOf(SEPARATOR_SUB_TYPE);
		if(seperatorIndex < 0) {
			// Separator not found
			return null;
		}
		// +1 because we want to skip the separator
		return fullType.substring(seperatorIndex+1);
		
	}
	
	public static String createSubTyped(@NonNull String type,@NonNull  String sub) {
		return String.join(SEPARATOR_SUB_TYPE, type, sub);
	}
	
	public static <T> Set<Class<? extends T>> listImplementations(Class<T> base) {
		CPSMap map = globalMap.get(base);
		if(map == null) {
			log.warn("No implementations for {}", base);
			return Collections.emptySet();
		}
		return (Set<Class<? extends T>>)(Set)map.getClasses();
	}
	
	public static Set<Pair<Class<?>, Class<?>>> listImplementations() {
		return globalMap
				.entrySet()
				.stream()
				.<Pair<Class<?>, Class<?>>>flatMap(e->
					e.getValue()
						.getClasses()
						.stream()
						.map(v->Pair.of(e.getKey(), v))
				)
				.collect(Collectors.toSet());
	}

	@Override
	public String idFromValueAndType(Object value, Class<?> suggestedType) {
		String result = cpsMap.getTypeIdForClass(suggestedType);
		if(result == null) {
			//check if other base
			CPSType anno = value.getClass().getAnnotation(CPSType.class);
			if(anno == null)
				throw new IllegalStateException("There is no id for the class "+suggestedType+" for "+baseType.getTypeName()+".");
			return anno.id();
		}
		CPSType anno = suggestedType.getAnnotation(CPSType.class);
		if(anno != null && anno.subTyped()) {
			return createSubTyped(result, ((SubTyped)value).getSubType());
		}
		return result;
	}
	
	@Override
	public String getDescForKnownTypeIds() {
		return new TreeSet<>(cpsMap.getTypeIds()).toString();
	}
	
	@Override
	public String idFromValue(Object value) {
		return idFromValueAndType(value, value.getClass());
	}

	@Override
	public String idFromBaseType() {
		return "DEFAULT";
	}

	@Override
	public Id getMechanism() {
		return Id.CUSTOM;
	}
}