package com.bakdata.eva.handler;

import org.apache.commons.lang3.tuple.Pair;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.eva.model.Base;
import com.bakdata.eva.model.Group;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimap;

import io.github.classgraph.AnnotationClassRef;
import io.github.classgraph.ArrayTypeSignature;
import io.github.classgraph.BaseTypeSignature;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.ScanResult;
import io.github.classgraph.TypeSignature;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.bakdata.eva.Constants.*;

@Slf4j
@RequiredArgsConstructor
public class GroupHandler {
	private final ScanResult scan;
	private final Group group;
	private final SimpleWriter out;
	private Multimap<Base, Pair<CPSType, ClassInfo>> content = HashMultimap.create();
	
	public void handle() throws IOException {
		out.heading(group.getName());
		out.paragraph("This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.");
		out.paragraph("Instead of a list ConQuery also always accepts a single element.");
		
		for(var base : group.getBases()) {
			content.putAll(
				base,
				scan
					.getAllClasses()
					.stream()
					//all classes that are CPSType
					.filter(c-> c.hasAnnotation(CPS_TYPE))
					//resolve multiple CPSType annotations
					.flatMap(c -> Arrays
						.stream(c.loadClass().getAnnotationsByType(CPSType.class))
						.map(anno -> Pair.of(anno, c))
					)
					//only classes that have the current base as a base
					.filter(p -> p.getLeft()
						.base()
						.equals(base.getBaseClass())
					)
					.collect(Collectors.toList())
			);
		}
		for(var base : group.getBases()) {
			handleBase(base);
		}
	}
	
	public void handleBase(Base base) throws IOException {
		out.subHeading(baseTitle(base.getBaseClass()));
		out.paragraph(base.getDescription());
		String typeProperty = base.getBaseClass().getAnnotation(JsonTypeInfo.class).property();
		
		out.paragraph("Different types of "+base.getBaseClass().getSimpleName()+" can be used by setting `"+typeProperty+"` to one of the following values:");

		for(Pair<CPSType, ClassInfo> pair : content.get(base).stream().sorted(Comparator.comparing(p->p.getLeft().id())).collect(Collectors.toList())) {
			
			handleClass(base, pair.getLeft(), pair.getRight());
		}
			
		out.line("\n");
	}
	
	private String baseTitle(Class<?> baseClass) {
		return "Base "+baseClass.getSimpleName();
	}

	public void handleClass(Base base, CPSType anno, ClassInfo c) throws IOException {
		String id = anno.id();
		out.subSubHeading(id);
		out.paragraph("Java Name: `"+c.getName()+"`");
		if(c.getFieldInfo().stream().anyMatch(this::isJSONSettableField)) {
			out.line("The following fields are supported:");

			out.tableHeader("Field", "Type");
			for(var field : c.getFieldInfo().stream().sorted().collect(Collectors.toList())) {
				handleField(field);
			}
		}
		else {
			out.paragraph("No fields can be set for this type.");
		}
	}

	private void handleField(FieldInfo field) throws IOException {
		if(!isJSONSettableField(field)) {
			return;
		}
		
		String name = field.getName();
		String type = printType(Objects.requireNonNullElse(
			field.getTypeSignature(),
			field.getTypeDescriptor()
		));
		if(ID_REF.stream().anyMatch(field::hasAnnotation)) {
			type = "ID of "+type;
		}
		if(ID_REF_COL.stream().anyMatch(field::hasAnnotation)) {
			type = "list of ID of "+type;
		}
		

		out.table(
			name,
			type
		);
	}

	private String printType(TypeSignature type) {
		if(type instanceof ArrayTypeSignature) {
			return "list of "+printType(((ArrayTypeSignature) type).getElementTypeSignature());
		}
		if(type instanceof BaseTypeSignature) {
			return "`"+type.toString()+"`";
		}
		if(type instanceof ClassRefTypeSignature) {
			var classRef = (ClassRefTypeSignature) type;
			Class<?> cl = classRef.loadClass();
			if(IId.class.isAssignableFrom(cl)) {
				String name = cl.getSimpleName();
				return "ID of `"+name.substring(0,name.length()-2)+"`";
			}
			
			if(Collection.class.isAssignableFrom(cl)) {
				//sadly we do not have all the infos here
				return "`"+classRef.toStringWithSimpleNames()+"`";
			}

			if(String.class.isAssignableFrom(cl)) {
				return "`String`";
			}
			
			if(content.keySet().stream().map(Base::getBaseClass).anyMatch(c->c.equals(cl))) {
				return "["+type.toStringWithSimpleNames()+"]("+anchor(baseTitle(cl))+")";
			}
			
			if(Enum.class.isAssignableFrom(cl)) {
				return "one of "+Arrays.stream(cl.getEnumConstants()).map(Enum.class::cast).map(Enum::name).collect(Collectors.joining(", "));
			}
				
		}
		log.warn("Unhandled type {}", type);
		return "`"+type.toStringWithSimpleNames()+"`";
	}

	private String anchor(String str) {
		return "#"+StringUtils.replaceChars(str, ' ', '-');
	}

	private boolean isJSONSettableField(FieldInfo field) {
		if(field.hasAnnotation(JSON_IGNORE) || field.isStatic() || field.hasAnnotation(JSON_BACK_REFERENCE)) {
			return false;
		}
		//has setter
		if(field.getClassInfo().hasMethod("set"+StringUtils.capitalize(field.getName()))) {
			return true;
		}
		//has @JsonCreator
		for(var method : field.getClassInfo().getMethodAndConstructorInfo()) {
			if(method.hasAnnotation(JSON_CREATOR)) {
				if(Arrays.stream(method.getParameterInfo()).anyMatch(param->param.getName().equals(field.getName()))) {
					return true;
				}
			}
		}

		return false;
	}
}
