package com.bakdata.eva.forms.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.SelectDescriptor;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "type")
@CPSBase
@Slf4j
@Getter
@Setter
public abstract class Form {

	@NonNull
	@JsonInclude
	private UUID id = UUID.randomUUID();

	@JsonIgnore
	private List<ColumnDescriptor> columns = new ArrayList<>();
	@JsonIgnore
	private transient FixedColumn[] fixedFeatures;

	@JsonIgnore
	public abstract ColumnNamer getColumnNamer();

	public Form(FixedColumn... fixedFeatures) {
		this.fixedFeatures = fixedFeatures;
	}

	@JsonIgnore
	protected String[] getAdditionalHeader() {
		return new String[0];
	}

	@JsonIgnore
	public abstract List<FeatureGroupDescription<?>> getFeatureGroupDescriptions();

	@JsonIgnore
	public List<FeatureDescription<?>> getFeatureDescriptions() {
		return getFeatureGroupDescriptions()
			.stream()
			.flatMap(FeatureGroupDescription::streamFeatureDescriptions)
			.collect(Collectors.toList());
	}

	public static final Comparator<ConceptElementId<?>> CEID_COMPARATOR = new Comparator<ConceptElementId<?>>() {

		@Override
		public int compare(ConceptElementId<?> o1, ConceptElementId<?> o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};

	public abstract ManagedQuery executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException, IOException;

	@JsonIgnore
	public abstract Collection<ManagedExecutionId> getUsedQueries();

	public void init(Namespaces namespaces, User user) {
		// add fixed columns
		for (FixedColumn f : fixedFeatures) {
			try {
				ConceptElement<?> n = f.resolve(namespaces);

				for (FeatureGroupDescription<?> fd : getFeatureGroupDescriptions()) {
					if (f.getOccurringGroup() == fd.getGroupType()) {
						CQConcept c = new CQConcept();
						c.setLabel(n.getName());
						c.setIds(Arrays.asList(n.getId()));
						c.setTables(new ArrayList<>());
						for (Connector con : n.getConcept().getConnectors()) {
							CQTable t = new CQTable();
							t.setId(con.getId());
							t.setConcept(c);
							t.setFilters(Collections.emptyList());
							c.getTables().add(t);
						}
						if (f.getConceptConfiguration() != null)
							f.getConceptConfiguration().accept(c);
						fd.addFixedFeature(c);
					}
				}
			}
			catch (IllegalStateException | NoSuchElementException e) {
				log.error("Failed to resolve fixed column " + f.getKey(), e);
				throw e;
			}
		}

		// Workaround aslong as boolean selects cannot be combined
		// Condense CQ-ORs to one Child if they all belong to one concept
		for (FeatureGroupDescription<?> fd : getFeatureGroupDescriptions()) {
			for (CQOr or : fd.getFeatures()) {
				// Check all ids contained in an or, if they belong to the same concept by counting the Concepts
				long count = or
					.getChildren()
					.stream()
					.filter(CQConcept.class::isInstance)
					.map(CQConcept.class::cast)
					.map(CQConcept::getIds)
					.flatMap(List::stream)
					.map(ConceptElementId::findConcept)
					.distinct()
					.count();
				if(count != 1) {
					throw new IllegalStateException("Cannot condense the OR node, because different concepts have been used:" + or);
				}
				// Only check first level for now and condense if there are more than one child
				if( or.getChildren().size() > 1) {
					List<ConceptElementId<?>> allIds = or
					.getChildren()
					.stream()
					.filter(CQConcept.class::isInstance)
					.map(CQConcept.class::cast)
					.map(CQConcept::getIds)
					.flatMap(List::stream)
					.collect(Collectors.toList());
					
					// remove all but the first
					or.setChildren(or.getChildren().subList(0, 1));
					
					// set all ids in the first element
					((CQConcept)or.getChildren().get(0)).setIds(allIds);
				}
			}
		}

		// add default selects if none were selected in each Concept
		for (FeatureGroupDescription<?> fd : getFeatureGroupDescriptions()) {
			for (CQOr or : fd.getFeatures()) {
				for (CQElement e : or.getChildren()) {
					if (e instanceof CQConcept) {
						CQConcept concept = (CQConcept) e;
						if (concept.getSelects().isEmpty()) {
							Concept<?> resolved = namespaces.resolve(concept.getIds().get(0).findConcept());
							if (!resolved.getSelects().isEmpty()) {
								concept.getSelects().add(resolved.getSelects().get(0));
							}

							else {
								try {
									List<Select> connectorSelects = resolved
										.getConnectors()
										.stream()
										.map(Connector::getSelects)
										.flatMap(List::stream)
										.collect(Collectors.toList());
									concept.getSelects().add(connectorSelects.get(0));
								}
								catch (IndexOutOfBoundsException ex) {
									throw new IllegalStateException(
										"For concept"
											+ resolved.getId()
											+ " neither the concept or its connectors had any selects defined. Cannot define a column for this concept if there is no select.");
								}
							}
						}
					}
				}
			}
		}

		// Initialize column name creator
		Function<SelectDescriptor, String> namer = this.getColumnNamer().getNamer();

		// clean and create column descriptors
		for (FeatureGroupDescription<?> fd : getFeatureGroupDescriptions()) {
			// clean feature and outcome list
			fd.getFeatures().removeIf(g -> g.getChildren().isEmpty());

			for (int i = 0; i < fd.getFeatures().size(); i++) {
				CQOr feature = fd.getFeatures().get(i);
				CQConcept c = (CQConcept) feature.getChildren().get(0);
				for(Select select : c.getSelects()) {
					ResultType selectorType = select.getResultType();
					ArrayList<ConceptElementId<?>> nodes = new ArrayList<>(c.getIds());
					// Sort nodes for deterministic column names
					nodes.sort(CEID_COMPARATOR);
					// In order to generate the same output as before (when only a single 'QConcept'
					// was allowed), use
					// the description of the first concept in the concept list for the generated
					// column descriptor
					String description = nodes
						.stream()
						.map(ce -> namespaces.get(ce.getDataset()).getStorage().getConcept(ce.findConcept()).getElementById(ce))
						.map(ConceptElement::getDescription)
						.collect(Collectors.joining(" or "));
	
					// Suppress ugly "null" description
					if (description.equals("null")) {
						description = null;
					}
	
					// If ConceptTree, get the root for the StatisticServer
					Concept<?> concept = select.getHolder().findConcept();
					String root = null;
					if (concept instanceof TreeConcept) {
						root = concept.getLabel();
					}
	
					ColumnDescriptor column = ColumnDescriptor
						.builder()
						.label(c.getLabel())
						.description(description)
						.column(fd.getGroupType().getPrefix() + namer.apply(new SelectDescriptor(select, c)))
						.type(selectorType)
						.rootConcept(root)
						.build();
	
					// Allow the form to manipulate the column, to set individual properties (e.g.
					// MatchingType, BaseCondition-Flag)
					BiConsumer<ColumnDescriptor, CQOr> manipulator;
					if ((manipulator = fd.getColumnManipulator()) != null) {
						manipulator.accept(column, feature);
					}
	
					columns.add(column);
				}

			}
		}
	}
}