package com.bakdata.conquery.io.cps;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CPSTypeIdResolver implements TypeIdResolver {

	private static HashMap<Class<?>, CPSMap> globalMap;
	
	private JavaType baseType;
	private CPSMap cpsMap;

	@Override
	public void init(JavaType baseType) {
		this.baseType = baseType;
		this.cpsMap = null;
		//see #145  ideally this would create an aggregate map of all the children and not just super classes
		Class<?> cl = baseType.getRawClass();
		while(cpsMap == null && cl != null) {
			cpsMap = globalMap.get(cl);
			cl = cl.getSuperclass();
		}
		//if there was still none found we have to for empty
		if(cpsMap == null) {
			cpsMap = CPSMap.getEMPTY();
		}
	}

	static {
		log.info("Scanning Classpath");
		//scan classpaths for annotated child classes
		
		ScanResult scanRes = new ClassGraph()
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
		
		log.info("Scanned: {} classes in classpath", scanRes.getAllClasses().size());
		Set<Class<?>> types = new HashSet<>();
		types.addAll(scanRes.getClassesWithAnnotation(CPSTypes.class.getName()).loadClasses());
		types.addAll(scanRes.getClassesWithAnnotation(CPSType.class.getName()).loadClasses());
		
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
				
				map.add(anno.id(), type);
			}
		}
		
		List<Class<?>> bases = scanRes.getClassesWithAnnotation(CPSBase.class.getName()).loadClasses();
		for(Class<?> b:bases) {
			CPSMap map = globalMap.get(b);
			if(map==null) {
				log.warn("\tBase Class {}:\tNo registered types", b);
			}
			else {
				log.info("\tBase Class {}", b);
				map.calculateInverse();
				for(Entry<Class<?>, String> e:map) {
					log.info("\t\t{}\t->\t{}", e.getValue(), e.getKey());
				}
			}
			
		}
	}

	@Override
	public JavaType typeFromId(DatabindContext context, String id) {
		Class<?> result = cpsMap.getClassFromId(id);
		if(result == null) {
			throw new IllegalStateException("There is no type "+id+" for "+baseType.getTypeName()+". Try: "+getDescForKnownTypeIds());
		}
		else {
			return TypeFactory.defaultInstance().constructSpecializedType(baseType, result);
		}
	}
	
	public static Set<Class<?>> listImplementations(Class<?> base) {
		return globalMap.get(base).getClasses();
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
			else
				return anno.id();
		}
		else {
			return result;
		}
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