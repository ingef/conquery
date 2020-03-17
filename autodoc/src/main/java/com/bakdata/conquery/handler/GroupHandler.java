package com.bakdata.conquery.handler;

import static com.bakdata.conquery.Constants.*;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.introspection.Introspection;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.model.Base;
import com.bakdata.conquery.model.Group;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.util.PrettyPrinter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Primitives;
import io.github.classgraph.ArrayTypeSignature;
import io.github.classgraph.BaseTypeSignature;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;
import io.github.classgraph.TypeArgument;
import io.github.classgraph.TypeParameter;
import io.github.classgraph.TypeSignature;
import io.github.classgraph.TypeVariableSignature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@RequiredArgsConstructor
public class GroupHandler {
	private final ScanResult scan;
	private final Group group;
	private final SimpleWriter out;
	private final File root;
	private Multimap<Base, Pair<CPSType, ClassInfo>> content = HashMultimap.create();
	private List<Pair<String, MethodInfo>> endpoints = new ArrayList<>();
	
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
		
		for(var resource : group.getResources()) {
			collectEndpoints(resource);
		}
		if(!endpoints.isEmpty()) {
			out.heading("REST endpoints");
			for(var endpoint : endpoints.stream().sorted(Comparator.comparing(Pair::getLeft)).collect(Collectors.toList())) {
				handleEndpoint(endpoint.getLeft(), endpoint.getRight());
			}
		}
		
		for(var base : group.getBases()) {
			handleBase(base);
		}
		
		out.subHeading("Other Types");
		for(var t : group.getOtherClasses().stream().sorted(Comparator.comparing(Class::getSimpleName)).collect(Collectors.toList())) {
			handleClass(typeTitle(t), scan.getClassInfo(t.getName()));
		}
		
		if(!group.getMarkerInterfaces().isEmpty()) {
			out.subHeading("Marker Interfaces");
			for(var t : group.getMarkerInterfaces().stream().sorted(Comparator.comparing(Class::getSimpleName)).collect(Collectors.toList())) {
				handleMarkerInterface(markerTitle(t), scan.getClassInfo(t.getName()));
			}
		}
	}
	
	private void handleEndpoint(String url, MethodInfo method) throws IOException {
		Introspection introspec = Introspection.from(root, method.getClassInfo()).findMethod(method);
		try(var details = details(getRestMethod(method)+"\u2001"+url, method.getClassInfo(), introspec)) {
			out.paragraph("Method: "+code(method.getName()));
			for(var param : method.getParameterInfo()) {
				if(param.hasAnnotation(PATH_PARAM) || param.hasAnnotation(AUTH) || param.hasAnnotation(CONTEXT)) {
					continue;
				}
				out.line("Expects: "+ printType(new Ctx(), param.getTypeSignatureOrTypeDescriptor()));
			}
			out.paragraph("Returns: "+printType(new Ctx(), method.getTypeSignatureOrTypeDescriptor().getResultType()));
		}
	}

	private void collectEndpoints(Class<?> resource) throws IOException {
		ClassInfo info = scan.getClassInfo(resource.getName());
		
		for(var method : info.getMethodInfo()) {
			if(getRestMethod(method) == null) {
				continue;
			}
			
			UriBuilder builder = UriBuilder.fromResource(resource);
			if(method.hasAnnotation(PATH)) {
				builder = builder.path(resource, method.getName());
			}
			
			endpoints.add(Pair.of(builder.toTemplate(), method));
		}
	}

	private String getRestMethod(MethodInfo method) {
		for(var rest : RESTS) {
			if(method.hasAnnotation(rest)) {
				return method.getAnnotationInfo(rest).getClassInfo().getSimpleName();
			}
		}
		return null;
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
		var source = Introspection.from(root, c); 
		try(var details = details(name, c, source)) {
			if(c.getFieldInfo().stream().anyMatch(this::isJSONSettableField)) {
				out.line("Supported Fields:");
	
				out.tableHeader("", "Field", "Type", "Default", "Example", "Description");
				for(var field : c.getFieldInfo().stream().sorted().collect(Collectors.toList())) {
					handleField(c, field);
				}
			}
			else {
				out.paragraph("No fields can be set for this type.");
			}
		}
	}
	
	private Closeable details(String name, ClassInfo c, Introspection source) throws IOException {
		out.subSubHeading(
			name
			//non-ASCII characters and tags do not change the anchor
			+ "<sup><sub><sup>\u2001"+editLink(source)+"</sup></sub></sup>"
		);
		out.paragraph(source.getDescription());
		out.paragraph("<details><summary>Details</summary><p>");
		
		out.paragraph("Java Type: "+code(c.getName()));
		if(!Strings.isNullOrEmpty(source.getExample())) {
			out.paragraph(
				"Example:\n\n```jsonc\n"
				+ PrettyPrinter.print(source.getExample())
				+ "\n```"
			);
		}
		
		return new Closeable() {
			@Override
			public void close() throws IOException {
				out.line("</p></details>");
			}
		};
	}
	
	private void handleMarkerInterface(String name, ClassInfo c) throws IOException {
		var source = Introspection.from(root, c); 
		try(var details = details(name, c, source)) {			
			Set<String> values = new HashSet<>();
			for(var cl : group.getOtherClasses()) {
				if(c.loadClass().isAssignableFrom(cl)) {
					values.add("["+cl.getSimpleName()+"]("+anchor(typeTitle(cl))+")");
				}
			}
			content
				.values()
				.stream()
				.filter(p-> c.loadClass().isAssignableFrom(p.getRight().loadClass()))
				.forEach(p->values.add("["+p.getLeft().id()+"]("+anchor(p.getLeft().id())+")"));
			for(var cl : group.getMarkerInterfaces()) {
				if(c.loadClass().isAssignableFrom(cl) && !c.loadClass().equals(cl)) {
					values.add("["+cl.getSimpleName()+"]("+anchor(typeTitle(cl))+")");
				}
			}
	
			if(!values.isEmpty()) {
				out.paragraph (
					"A "+name+" is any of:\n* "
					+ values.stream().sorted().collect(Collectors.joining("\n* "))
				);
			}
		}
	}

	private void handleField(ClassInfo currentType, FieldInfo field) throws IOException {
		if(!isJSONSettableField(field)) {
			return;
		}
		
		var introspec = Introspection.from(root, field.getClassInfo()).findField(field);
		String name = field.getName();
		var typeSignature = field.getTypeSignatureOrTypeDescriptor();
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
		
		out.table(
			editLink(introspec),
			name,
			type,
			findDefault(currentType, field),
			introspec.getExample(),
			introspec.getDescription()
		);
	}

	private String findDefault(ClassInfo currentType, FieldInfo field) {
		try {
			Object value = currentType.loadClass().getConstructor().newInstance();
			var node = Jackson.MAPPER.valueToTree(value);
			var def = node.get(field.getName());
			if(def == null) {
				return "\u2400";
			}
			else {
				String json = Jackson.MAPPER.writeValueAsString(def);
				//we don't want to print defaults if it is a whole object itself
				if(json.contains("{")) {
					return "";
				}
				//check if file path not not generate absolute paths
				String localPath = Jackson.MAPPER.writeValueAsString(new File("."));
				json = StringUtils.replace(json, localPath.substring(1,localPath.length()-2), "./");
				return code(json);
			}
		}
		catch(Exception e) {
			return "?";
		}
	}

	private String editLink(Introspection intro) throws IOException {
		var target = root.toPath().relativize(intro.getFile().getCanonicalFile().toPath());
		String line = intro.getLine();
		return "[✎]("
			+ "https://github.com/bakdata/conquery/edit/develop/"
			+ FilenameUtils.separatorsToUnix(target.toString())
			+ (line==null?"":"#"+line)
			+ ")";
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
			
			//File
			if(File.class.isAssignableFrom(cl)) {
				//we could check if dir or file here
				return code("File");
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
			//another contentClass
			var match=content.values().stream().filter(p->p.getRight().loadClass().equals(cl)).collect(MoreCollectors.toOptional());
			if(match.isPresent()) {
				return "["+match.get().getLeft().id()+"]("+anchor(match.get().getLeft().id())+")";
			}
			
			if(content.keySet().stream().map(Base::getBaseClass).anyMatch(c->c.equals(cl))) {
				return "["+type.toStringWithSimpleNames()+"]("+anchor(baseTitle(cl))+")";
			}
			//another class in the group
			if(group.getOtherClasses().contains(cl)) {
				return "["+cl.getSimpleName()+"]("+anchor(typeTitle(cl))+")";
			}
			//a marker interface
			if(group.getMarkerInterfaces().contains(cl)) {
				return "["+cl.getSimpleName()+"]("+anchor(markerTitle(cl))+")";
			}
			
			//ENUM
			if(Enum.class.isAssignableFrom(cl)) {
				return "one of "+Arrays.stream(cl.getEnumConstants()).map(Enum.class::cast).map(Enum::name).collect(Collectors.joining(", "));
			}
			
			if(Primitives.isWrapperType(cl)) {
				return "`"+Primitives.unwrap(cl).getSimpleName()+"`"
					+ (ctx.isIdOf()?"":" or `null`");
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

	private String markerTitle(Class<?> cl) {
		return "Marker "+cl.getSimpleName();
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
