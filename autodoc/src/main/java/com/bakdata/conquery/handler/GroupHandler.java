package com.bakdata.conquery.handler;

import static com.bakdata.conquery.Constants.*;
import static com.bakdata.conquery.Constants.DOC;
import static com.bakdata.conquery.Constants.ID_REF;
import static com.bakdata.conquery.Constants.ID_REF_COL;
import static com.bakdata.conquery.Constants.JSON_BACK_REFERENCE;
import static com.bakdata.conquery.Constants.JSON_CREATOR;
import static com.bakdata.conquery.Constants.JSON_IGNORE;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.model.Base;
import com.bakdata.conquery.model.CreateableDoc;
import com.bakdata.conquery.model.Group;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.util.Doc;
import com.bakdata.conquery.util.PrettyPrinter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Primitives;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ArrayTypeSignature;
import io.github.classgraph.BaseTypeSignature;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.ScanResult;
import io.github.classgraph.TypeArgument;
import io.github.classgraph.TypeParameter;
import io.github.classgraph.TypeSignature;
import io.github.classgraph.TypeVariableSignature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
		if(group.getDescription()!=null) {
			out.paragraph(group.getDescription());
		}
		
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
		
		out.subHeading("Other Types");
		for(var t : group.getOtherClasses().stream().sorted(Comparator.comparing(Class::getSimpleName)).collect(Collectors.toList())) {
			handleClass(typeTitle(t), scan.getClassInfo(t.getName()));
		}
	}
	
	public void handleBase(Base base) throws IOException {
		out.subHeading(baseTitle(base.getBaseClass()));
		out.paragraph(base.getDescription());
		String typeProperty = base.getBaseClass().getAnnotation(JsonTypeInfo.class).property();
		
		out.paragraph("Different types of "+base.getBaseClass().getSimpleName()+" can be used by setting "+code(typeProperty)+" to one of the following values:");

		for(Pair<CPSType, ClassInfo> pair : content.get(base).stream().sorted(Comparator.comparing(p->p.getLeft().id())).collect(Collectors.toList())) {
			
			handleClass(pair.getLeft(), pair.getRight());
		}
			
		out.line("\n");
	}
	
	private String baseTitle(Class<?> baseClass) {
		return "Base "+baseClass.getSimpleName();
	}

	public void handleClass(CPSType anno, ClassInfo c) throws IOException {
		if(group.getHides().contains(c.loadClass())) {
			return;
		}
		handleClass(anno.id(), c);
	}

	private void handleClass(String name, ClassInfo c) throws IOException {
		out.subSubHeading(name);
		out.paragraph("Java Type: "+code(c.getName()));
		Doc docAnnotation = getDocAnnotation(c.getAnnotationInfo(DOC));
		out.paragraph(editLink(c)+" "+docAnnotation.description());
		if(!Strings.isNullOrEmpty(docAnnotation.example())) {
			out.paragraph(
				"Example:\n\n```jsonc\n"
				+ PrettyPrinter.print(docAnnotation.example())
				+ "\n```"
			);
		}
		
		if(c.getFieldInfo().stream().anyMatch(this::isJSONSettableField)) {
			out.line("The following fields are supported:");

			out.tableHeader("", "Field", "Type", "Example", "Description");
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
		var typeSignature = Objects.requireNonNullElse(
			field.getTypeSignature(),
			field.getTypeDescriptor()
		);
		Ctx ctx = new Ctx().withField(field);
		
		String type;
		if(ID_REF.stream().anyMatch(field::hasAnnotation)) {
			type = ID_OF+printType(ctx.withIdOf(true), typeSignature);
		}
		else if(ID_REF_COL.stream().anyMatch(field::hasAnnotation)) {
			type = LIST_OF+ID_OF+StringUtils.removeStart(printType(ctx.withIdOf(true), typeSignature), LIST_OF);
		}
		else {
			type = printType(ctx, typeSignature);
		}
		
		Doc docAnnotation = getDocAnnotation(field.getAnnotationInfo(DOC));

		out.table(
			editLink(field.getClassInfo()),
			name,
			type,
			docAnnotation.example(),
			docAnnotation.description()
		);
	}

	private Doc getDocAnnotation(AnnotationInfo info) {
		if(info == null) {
			return new CreateableDoc("", "");
		}
		return (Doc)info.loadClassAndInstantiate();
		//String description = docAnnotation==null ? "" : docAnnotation.getParameterValues().get("description").getValue().toString();
		//String example = docAnnotation==null ? "" : docAnnotation.getParameterValues().get("example").getValue().toString();
	}

	private String editLink(ClassInfo classInfo) {
		return "[✎]("
			+"https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/"
			+ classInfo.getName().replace('.', '/')
			+".java)";
	}

	private String printType(Ctx ctx, TypeSignature type) {
		if(type instanceof ArrayTypeSignature) {
			return LIST_OF+printType(ctx, ((ArrayTypeSignature) type).getElementTypeSignature());
		}
		if(type instanceof BaseTypeSignature) {
			return code(type.toString());
		}
		if(type instanceof ClassRefTypeSignature) {
			var classRef = (ClassRefTypeSignature) type;
			Class<?> cl = classRef.loadClass();
			
			//ID
			if(IId.class.isAssignableFrom(cl)) {
				String name = cl.getSimpleName();
				return ID_OF+code(name.substring(0,name.length()-2));
			}
			
			//Iterable
			if(Iterable.class.isAssignableFrom(cl)) {
				var param = classRef.getTypeArguments().get(0);
				return LIST_OF+printType(ctx.withGeneric(true), param);
			}
			
			//Map
			if(BiMap.class.isAssignableFrom(cl)) {
				return "bijective map from "
					+ printType(ctx.withGeneric(true), classRef.getTypeArguments().get(0))
					+ " to "
					+ printType(ctx.withGeneric(true), classRef.getTypeArguments().get(1));
			}
			if(Map.class.isAssignableFrom(cl)) {
				return "map from "
					+ printType(ctx.withGeneric(true), classRef.getTypeArguments().get(0))
					+ " to "
					+ printType(ctx.withGeneric(true), classRef.getTypeArguments().get(1));
			}
			

			//String
			if(String.class.isAssignableFrom(cl)) {
				return code("String");
			}
			
			//another BaseClass
			if(content.keySet().stream().map(Base::getBaseClass).anyMatch(c->c.equals(cl))) {
				return "["+type.toStringWithSimpleNames()+"]("+anchor(baseTitle(cl))+")";
			}
			//another class in the group
			if(group.getOtherClasses().contains(cl)) {
				return "["+cl.getSimpleName()+"]("+anchor(typeTitle(cl))+")";
			}
			
			//ENUM
			if(Enum.class.isAssignableFrom(cl)) {
				return "one of "+Arrays.stream(cl.getEnumConstants()).map(Enum.class::cast).map(Enum::name).collect(Collectors.joining(", "));
			}
			
			if(Primitives.isWrapperType(cl)) {
				return Primitives.unwrap(cl).getSimpleName()
					+ (ctx.isIdOf()?"":" or null");
			}
			
			//default for hidden types
			if(group.getHides().contains(cl)) {
				return code(type.toStringWithSimpleNames());
			}
		}
		if(!ctx.isIdOf()) {
			log.warn("Unhandled type {}", type);
		}
		return code(type.toStringWithSimpleNames());
	}

	private String code(String string) {
		return "`"+string+"`";
	}

	private String typeTitle(Class<?> cl) {
		return "Type "+cl.getSimpleName();
	}

	private String printType(Ctx ctx, TypeArgument type) {
		if(type.getTypeSignature() == null) {
			return "UNKNWON";
		}
		if(type.getTypeSignature() instanceof TypeVariableSignature) {
			String v = type.getTypeSignature().toString();
			TypeParameter typeParam = ctx
				.getField()
				.getClassInfo()
				.getTypeSignature()
				.getTypeParameters()
				.stream()
				.filter(tp -> tp.getName().equals(v))
				.collect(MoreCollectors.onlyElement());
			if(typeParam.getClassBound()!=null) {
				return printType(ctx, typeParam.getClassBound());
			}
			else {
				return printType(ctx, typeParam.getInterfaceBounds().get(0));
			}
		}
		
		return printType(ctx, type.getTypeSignature());
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
