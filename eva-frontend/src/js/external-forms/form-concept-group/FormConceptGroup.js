// @flow

import React from "react";
import styled from "@emotion/styled";
import { connect, type Dispatch } from "react-redux";
import { type FieldPropsType } from "redux-form";
import difference from "lodash.difference";

import { resetAllFiltersInTables } from "conquery/lib/js/model/table";
import { nodeHasActiveFilters } from "conquery/lib/js/model/node";

import { CATEGORY_TREE_NODE } from "conquery/lib/js/common/constants/dndTypes";
import Dropzone from "conquery/lib/js/form-components/Dropzone";

import {
  getConceptById,
  getConceptsByIdsWithTablesAndSelects,
  hasConceptChildren
} from "conquery/lib/js/category-trees/globalTreeStoreHelper";
import { withDefaultValues } from "conquery/lib/js/standard-query-editor/reducer";

import DynamicInputGroup from "../../form-components/DynamicInputGroup";
import DropzoneList from "../../form-components/DropzoneList";

import { FormQueryNodeEditor } from "../form-query-node-editor";

import FormConceptNode from "./FormConceptNode";

type PropsType = FieldPropsType & {
  name: string,
  label: string,
  datasetId: string,
  onDropFilterFile: Function,
  newValue?: Object,
  disallowMultipleColumns?: boolean,
  isValidConcept?: Function
};

const addValue = (value, newValue) => [...value, newValue];

const removeValue = (value, valueIdx) => {
  return [...value.slice(0, valueIdx), ...value.slice(valueIdx + 1)];
};

const onToggleIncludeSubnodes = (
  value,
  valueIdx,
  conceptIdx,
  includeSubnodes
) => {
  const feature = value[valueIdx];
  const concepts = feature.concepts;
  const formConcept = concepts[conceptIdx];
  const concept = getConceptById(formConcept.ids);

  const childIds = [];
  const elements = concept.children.map(childId => {
    const child = getConceptById(childId);
    childIds.push(childId);
    return {
      matchingType: feature.matchingType,
      concepts: [
        {
          ids: [childId],
          label: child.label,
          tables: formConcept.tables,
          tree: formConcept.tree
        }
      ]
    };
  });

  if (includeSubnodes) value.splice(valueIdx + 1, 0, ...elements);
  else
    value = value.filter(f =>
      f.concepts.some(c => {
        return !(difference(c.ids, childIds).length === 0);
      })
    );

  return setConceptProperties(value, valueIdx, conceptIdx, { includeSubnodes });
};

// TODO: Re-implement
const addConceptFromFile = (
  label,
  rootConcepts,
  resolutionResult,
  value,
  newValue,
  valueIdx,
  conceptIdx
) => {
  const { selectedRoot, conceptList, filter } = resolutionResult;
  const rootConcept = getConceptById(selectedRoot);
  const tables = rootConcept.tables;

  let item;

  if (conceptList) {
    const lookupResult = getConceptsByIdsWithTablesAndSelects(
      conceptList,
      rootConcepts
    );

    if (lookupResult)
      item = {
        label,
        ids: conceptList,
        selects: lookupResult.selects,
        tables: lookupResult.tables,
        tree: lookupResult.root
      };
  } else if (filter) {
    const tableIdx = rootConcept.tables
      .map(t => {
        return t.id;
      })
      .indexOf(filter.tableId);
    const filters = rootConcept.tables[tableIdx].filters;
    const filterIdx = filters
      .map(t => {
        return t.id;
      })
      .indexOf(filter.filterId);

    item = {
      ids: [rootConcept.id || selectedRoot],
      label: label,
      tables: [
        ...tables.slice(0, tableIdx),
        {
          ...tables[tableIdx],
          filters: [
            ...filters.slice(0, filterIdx),
            {
              ...filters[filterIdx],
              value: filter.value,
              options: filter.value
            },
            ...filters.slice(filterIdx + 1)
          ]
        },
        ...tables.slice(tableIdx + 1)
      ],
      tree: selectedRoot
    };
  }

  if (conceptIdx) return setConcept(value, valueIdx, conceptIdx, item);
  else return addConcept(addValue(value, newValue), valueIdx, item);
};

const initializeConcept = item => {
  if (!item) return item;

  return {
    ...item,
    tables: withDefaultValues(item.tables),
    selects: withDefaultValues(item.selects)
  };
};

const addConcept = (value, valueIdx, item) => [
  ...value.slice(0, valueIdx),
  {
    ...value[valueIdx],
    concepts: [...value[valueIdx].concepts, initializeConcept(item)]
  },
  ...value.slice(valueIdx + 1)
];

const removeConcept = (value, valueIdx, conceptIdx) => [
  ...value.slice(0, valueIdx),
  {
    ...value[valueIdx],
    concepts: [
      ...value[valueIdx].concepts.slice(0, conceptIdx),
      ...value[valueIdx].concepts.slice(conceptIdx + 1)
    ]
  },
  ...value.slice(valueIdx + 1)
];

const setConcept = (value, valueIdx, conceptIdx, item) => [
  ...value.slice(0, valueIdx),
  {
    ...value[valueIdx],
    concepts: [
      ...value[valueIdx].concepts.slice(0, conceptIdx),
      item,
      ...value[valueIdx].concepts.slice(conceptIdx + 1)
    ]
  },
  ...value.slice(valueIdx + 1)
];

const setConceptProperties = (value, valueIdx, conceptIdx, props) =>
  setConcept(value, valueIdx, conceptIdx, {
    ...value[valueIdx].concepts[conceptIdx],
    ...props
  });

const toggleTable = (value, valueIdx, conceptIdx, tableIdx, isExcluded) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;

  return [
    ...value.slice(0, valueIdx),
    {
      ...value[valueIdx],
      concepts: [
        ...concepts.slice(0, conceptIdx),
        {
          ...concepts[conceptIdx],
          tables: [
            ...tables.slice(0, tableIdx),
            {
              ...tables[tableIdx],
              exclude: isExcluded
            },
            ...tables.slice(tableIdx + 1)
          ]
        },
        ...concepts.slice(conceptIdx + 1)
      ]
    },
    ...value.slice(valueIdx + 1)
  ];
};

const setFilterValue = (
  value,
  valueIdx,
  conceptIdx,
  tableIdx,
  filterIdx,
  filterValue,
  formattedFilterValue
) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;
  const filters = tables[tableIdx].filters;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    tables: [
      ...tables.slice(0, tableIdx),
      {
        ...tables[tableIdx],
        filters: [
          ...filters.slice(0, filterIdx),
          {
            ...filters[filterIdx],
            value: filterValue,
            formattedValue: formattedFilterValue
          },
          ...filters.slice(filterIdx + 1)
        ]
      },
      ...tables.slice(tableIdx + 1)
    ]
  });
};

const setSelects = (value, valueIdx, conceptIdx, selectedSelects) => {
  const concepts = value[valueIdx].concepts;
  const selects = concepts[conceptIdx].selects;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    // value contains the selects that have now been selected
    selects: selects.map(select => ({
      ...select,
      selected: !!selectedSelects.find(
        selectedValue => selectedValue.value === select.id
      )
    }))
  });
};

const setTableSelects = (
  value,
  valueIdx,
  conceptIdx,
  tableIdx,
  selectedSelects
) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;
  const selects = tables[tableIdx].selects;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    tables: [
      ...tables.slice(0, tableIdx),
      {
        ...tables[tableIdx],
        // value contains the selects that have now been selected
        selects: selects.map(select => ({
          ...select,
          selected: !!selectedSelects.find(
            selectedValue => selectedValue.value === select.id
          )
        }))
      },
      ...tables.slice(tableIdx + 1)
    ]
  });
};

const resetAllFilters = (value, valueIdx, conceptIdx) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    tables: resetAllFiltersInTables(tables)
  });
};

const switchFilterMode = (
  value,
  valueIdx,
  conceptIdx,
  tableIdx,
  filterIdx,
  mode
) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;
  const filters = tables[tableIdx].filters;

  return [
    ...value.slice(0, valueIdx),
    {
      ...value[valueIdx],
      concepts: [
        ...concepts.slice(0, conceptIdx),
        {
          ...concepts[conceptIdx],
          tables: [
            ...tables.slice(0, tableIdx),
            {
              ...tables[tableIdx],
              filters: [
                ...filters.slice(0, filterIdx),
                {
                  ...filters[filterIdx],
                  mode: mode,
                  value: null,
                  formattedValue: null
                },
                ...filters.slice(filterIdx + 1)
              ]
            },
            ...tables.slice(tableIdx + 1)
          ]
        },
        ...concepts.slice(conceptIdx + 1)
      ]
    },
    ...value.slice(valueIdx + 1)
  ];
};

const DropzoneListItem = styled("div")`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
`;

const FormConcept = (props: PropsType) => {
  // TODO: Re-enable
  // const dropTypeFile = props.enableDropFile ? NativeTypes.FILE : "";

  const newValue = props.newValue || { concepts: [] };

  return (
    <div>
      <DropzoneList
        label={props.label}
        dropzoneText={props.attributeDropzoneText}
        acceptedDropTypes={[CATEGORY_TREE_NODE]}
        disallowMultipleColumns={props.disallowMultipleColumns}
        onDelete={i => props.input.onChange(removeValue(props.input.value, i))}
        onDrop={(dropzoneProps, monitor) => {
          const item = monitor.getItem();

          if (props.isValidConcept && !props.isValidConcept(item)) return null;

          return props.input.onChange(
            addConcept(
              addValue(props.input.value, newValue),
              props.input.value.length, // Assuming the last index has increased after addValue
              item
            )
          );
        }}
        items={props.input.value.map((row, i) => (
          <DropzoneListItem>
            {props.renderRowPrefix
              ? props.renderRowPrefix(props.input, row, i)
              : null}
            <DynamicInputGroup
              key={i}
              onAddClick={() =>
                props.input.onChange(addConcept(props.input.value, i, null))
              }
              onRemoveClick={j =>
                props.input.onChange(
                  props.input.value &&
                    props.input.value[i].concepts.length === 1
                    ? removeValue(props.input.value, i)
                    : removeConcept(props.input.value, i, j)
                )
              }
              items={row.concepts.map((concept, j) =>
                concept ? (
                  <FormConceptNode
                    key={j}
                    valueIdx={i}
                    conceptIdx={j}
                    conceptNode={concept}
                    name={props.name}
                    hasActiveFilters={nodeHasActiveFilters(concept)}
                    onFilterClick={() =>
                      props.input.onChange(
                        setConceptProperties(props.input.value, i, j, {
                          isEditing: true
                        })
                      )
                    }
                    expand={{
                      onClick: () =>
                        props.input.onChange(
                          onToggleIncludeSubnodes(
                            props.input.value,
                            i,
                            j,
                            !concept.includeSubnodes
                          )
                        ),
                      expandable: hasConceptChildren(concept),
                      active: concept.includeSubnodes
                    }}
                  />
                ) : (
                  <Dropzone
                    acceptedDropTypes={[CATEGORY_TREE_NODE]}
                    onDrop={(dropzoneProps, monitor) => {
                      const item = monitor.getItem();

                      if (props.isValidConcept && !props.isValidConcept(item))
                        return null;

                      return props.input.onChange(
                        setConcept(
                          props.input.value,
                          i,
                          j,
                          initializeConcept(item)
                        )
                      );
                    }}
                  >
                    {props.conceptDropzoneText}
                  </Dropzone>
                )
              )}
            />
          </DropzoneListItem>
        ))}
      />
      <FormQueryNodeEditor
        formType={props.formType}
        fieldName={props.name}
        datasetId={props.datasetId}
        onCloseModal={(valueIdx, conceptIdx) =>
          props.input.onChange(
            setConceptProperties(props.input.value, valueIdx, conceptIdx, {
              isEditing: false
            })
          )
        }
        onUpdateLabel={(valueIdx, conceptIdx, label) =>
          props.input.onChange(
            setConceptProperties(props.input.value, valueIdx, conceptIdx, {
              label
            })
          )
        }
        onDropConcept={(valueIdx, conceptIdx, concept) => {
          const node = props.input.value[valueIdx].concepts[conceptIdx];
          props.input.onChange(
            setConceptProperties(props.input.value, valueIdx, conceptIdx, {
              ids: [...concept.ids, ...node.ids]
            })
          );
        }}
        onRemoveConcept={(valueIdx, conceptIdx, conceptId) => {
          const node = props.input.value[valueIdx].concepts[conceptIdx];
          props.input.onChange(
            setConceptProperties(props.input.value, valueIdx, conceptIdx, {
              ids: node.ids.filter(id => id !== conceptId)
            })
          );
        }}
        onToggleTable={(valueIdx, conceptIdx, tableIdx, isExcluded) =>
          props.input.onChange(
            toggleTable(
              props.input.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              isExcluded
            )
          )
        }
        onSelectSelects={(valueIdx, conceptIdx, selectedSelects) =>
          props.input.onChange(
            setSelects(props.input.value, valueIdx, conceptIdx, selectedSelects)
          )
        }
        onSetFilterValue={(
          valueIdx,
          conceptIdx,
          tableIdx,
          filterIdx,
          filterValue,
          formattedFilterValue
        ) =>
          props.input.onChange(
            setFilterValue(
              props.input.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              filterIdx,
              filterValue,
              formattedFilterValue
            )
          )
        }
        onSelectTableSelects={(
          valueIdx,
          conceptIdx,
          tableIdx,
          selectedSelects
        ) =>
          props.input.onChange(
            setTableSelects(
              props.input.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              selectedSelects
            )
          )
        }
        onSwitchFilterMode={(valueIdx, conceptIdx, tableIdx, filterIdx, mode) =>
          props.input.onChange(
            switchFilterMode(
              props.input.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              filterIdx,
              mode
            )
          )
        }
        onResetAllFilters={(valueIdx, conceptIdx) =>
          props.input.onChange(
            resetAllFilters(props.input.value, valueIdx, conceptIdx)
          )
        }
        onDropFiles={() => {
          console.log("Currently not supported");
        }}
        onToggleIncludeSubnodes={(valueIdx, conceptIdx, includeSubnodes) =>
          props.input.onChange(
            onToggleIncludeSubnodes(
              props.input.value,
              valueIdx,
              conceptIdx,
              includeSubnodes
            )
          )
        }
      />
    </div>
  );
};

// TODO: Re-enable file dropping
const mapDispatchToProps = (dispatch: Dispatch) => ({});

export const FormConceptGroup = connect(
  null,
  mapDispatchToProps
)(FormConcept);
