import * as React from "react";
import styled from "@emotion/styled";
import { useDispatch } from "react-redux";
import type { FieldProps } from "redux-form";
import T from "i18n-react";

import { resetAllFiltersInTables } from "../../model/table";
import { compose, includes } from "../../common/helpers/commonHelper";
import { nodeHasActiveFilters } from "../../model/node";

import {
  CONCEPT_TREE_NODE,
  FORM_CONCEPT_NODE,
} from "../../common/constants/dndTypes";
import DropzoneWithFileInput from "../../form-components/DropzoneWithFileInput";
import UploadConceptListModal from "../../upload-concept-list-modal/UploadConceptListModal";

import {
  getConceptById,
  getConceptsByIdsWithTablesAndSelects,
  hasConceptChildren,
} from "../../concept-trees/globalTreeStoreHelper";
import { tablesWithDefaults } from "../../model/table";
import { selectsWithDefaults } from "../../model/select";

import {
  initUploadConceptListModal,
  resetUploadConceptListModal,
} from "../../upload-concept-list-modal/actions";

import DynamicInputGroup from "../form-components/DynamicInputGroup";
import DropzoneList from "../form-components/DropzoneList";

import {
  initSelectsWithDefaults,
  initTablesWithDefaults,
} from "../transformers";

import type { ConceptListDefaults as ConceptListDefaultsType } from "../config-types";

import { FormQueryNodeEditor } from "../form-query-node-editor";

import FormConceptNode from "./FormConceptNode";
import FormConceptCopyModal from "./FormConceptCopyModal";
import { useAllowExtendedCopying } from "../stateSelectors";
import TransparentButton from "../../button/TransparentButton";

type PropsType = FieldProps & {
  fieldName: string;
  label: string;
  datasetId: string;
  onDropFilterFile: Function;
  newValue?: Object;
  isSingle?: boolean;
  disallowMultipleColumns?: boolean;
  blacklistedTables?: string[];
  whitelistedTables?: string[];
  defaults: ConceptListDefaultsType;
  isValidConcept?: Function;
};

const addValue = (value, newValue) => [...value, newValue];

const removeValue = (value, valueIdx) => {
  return [...value.slice(0, valueIdx), ...value.slice(valueIdx + 1)];
};

const onToggleIncludeSubnodes = (
  value,
  valueIdx,
  conceptIdx,
  includeSubnodes,
  newValue
) => {
  const element = value[valueIdx];
  const concept = element.concepts[conceptIdx];
  const conceptData = getConceptById(concept.ids);

  const childIds = [];
  const elements = conceptData.children.map((childId) => {
    const child = getConceptById(childId);

    childIds.push(childId);

    return {
      ...newValue,
      concepts: [
        {
          ids: [childId],
          label: child.label,
          description: child.description,
          tables: concept.tables,
          selects: concept.selects,
          tree: concept.tree,
        },
      ],
    };
  });

  const nextValue = includeSubnodes
    ? [
        ...value.slice(0, valueIdx + 1),
        // Insert right after the element
        ...elements,
        ...value.slice(valueIdx + 1),
      ]
    : value.filter((val) =>
        val.concepts.some((cpt) => {
          return childIds.every((childId) => !includes(cpt.ids, childId));
        })
      );

  return setConceptProperties(
    nextValue,
    nextValue.indexOf(element),
    conceptIdx,
    {
      includeSubnodes,
    }
  );
};

const createQueryNodeFromConceptListUploadResult = (
  label,
  rootConcepts,
  resolvedConcepts
) => {
  const lookupResult = getConceptsByIdsWithTablesAndSelects(
    resolvedConcepts,
    rootConcepts
  );

  return lookupResult
    ? {
        label,
        ids: resolvedConcepts,
        tables: lookupResult.tables,
        selects: lookupResult.selects,
        tree: lookupResult.root,
      }
    : null;
};

const addConceptsFromFile = (
  label,
  rootConcepts,
  resolvedConcepts,

  defaults,
  isValidConcept,

  value,
  newValue,

  valueIdx,
  conceptIdx = null
) => {
  const queryElement = createQueryNodeFromConceptListUploadResult(
    label,
    rootConcepts,
    resolvedConcepts
  );

  const concept = initializeConcept(queryElement, defaults);

  if (!concept || (!!isValidConcept && !isValidConcept(concept))) return value;

  if (conceptIdx === null) {
    return addConcept(addValue(value, newValue), valueIdx, concept);
  } else {
    return setConcept(value, valueIdx, conceptIdx, concept);
  }
};

const initializeConcept = (item, defaults) => {
  if (!item) return item;

  return compose(
    initSelectsWithDefaults(defaults.selects),
    initTablesWithDefaults(defaults.connectors)
  )({
    ...item,
    tables: tablesWithDefaults(item.tables),
    selects: selectsWithDefaults(item.selects),
  });
};

const addConcept = (value, valueIdx, item) => [
  ...value.slice(0, valueIdx),
  {
    ...value[valueIdx],
    concepts: [...value[valueIdx].concepts, item],
  },
  ...value.slice(valueIdx + 1),
];

const removeConcept = (value, valueIdx, conceptIdx) => [
  ...value.slice(0, valueIdx),
  {
    ...value[valueIdx],
    concepts: [
      ...value[valueIdx].concepts.slice(0, conceptIdx),
      ...value[valueIdx].concepts.slice(conceptIdx + 1),
    ],
  },
  ...value.slice(valueIdx + 1),
];

const setConcept = (value, valueIdx, conceptIdx, item) => [
  ...value.slice(0, valueIdx),
  {
    ...value[valueIdx],
    concepts: [
      ...value[valueIdx].concepts.slice(0, conceptIdx),
      item,
      ...value[valueIdx].concepts.slice(conceptIdx + 1),
    ],
  },
  ...value.slice(valueIdx + 1),
];

const setConceptProperties = (value, valueIdx, conceptIdx, props) =>
  setConcept(value, valueIdx, conceptIdx, {
    ...value[valueIdx].concepts[conceptIdx],
    ...props,
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
              exclude: isExcluded,
            },
            ...tables.slice(tableIdx + 1),
          ],
        },
        ...concepts.slice(conceptIdx + 1),
      ],
    },
    ...value.slice(valueIdx + 1),
  ];
};

const setDateColumn = (
  value,
  valueIdx,
  conceptIdx,
  tableIdx,
  dateColumnValue
) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    tables: [
      ...tables.slice(0, tableIdx),
      {
        ...tables[tableIdx],
        dateColumn: {
          ...tables[tableIdx].dateColumn,
          value: dateColumnValue,
        },
      },
      ...tables.slice(tableIdx + 1),
    ],
  });
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
            formattedValue: formattedFilterValue,
          },
          ...filters.slice(filterIdx + 1),
        ],
      },
      ...tables.slice(tableIdx + 1),
    ],
  });
};

const setSelects = (value, valueIdx, conceptIdx, selectedSelects) => {
  const concepts = value[valueIdx].concepts;
  const selects = concepts[conceptIdx].selects;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    // value contains the selects that have now been selected
    selects: selects.map((select) => ({
      ...select,
      selected: !selectedSelects
        ? false
        : !!selectedSelects.find(
            (selectedValue) => selectedValue.value === select.id
          ),
    })),
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
        selects: selects.map((select) => ({
          ...select,
          selected: !selectedSelects
            ? false
            : !!selectedSelects.find(
                (selectedValue) => selectedValue.value === select.id
              ),
        })),
      },
      ...tables.slice(tableIdx + 1),
    ],
  });
};

const resetAllFilters = (value, valueIdx, conceptIdx) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    tables: resetAllFiltersInTables(tables),
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
                  formattedValue: null,
                },
                ...filters.slice(filterIdx + 1),
              ],
            },
            ...tables.slice(tableIdx + 1),
          ],
        },
        ...concepts.slice(conceptIdx + 1),
      ],
    },
    ...value.slice(valueIdx + 1),
  ];
};

const copyConcept = (item) => {
  return JSON.parse(JSON.stringify(item));
};

const DropzoneListItem = styled("div")`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
`;

const SxTransparentButton = styled(TransparentButton)`
  margin-left: 10px;
`;

const FormConceptGroup = (props: PropsType) => {
  const newValue = props.newValue;
  const defaults = props.defaults || {};

  const [isCopyModalOpen, setIsCopyModalOpen] = React.useState(false);
  const allowExtendedCopying = useAllowExtendedCopying(props.fieldName);

  const dispatch = useDispatch();

  const initModal = (file) => dispatch(initUploadConceptListModal(file));
  const resetModal = () => dispatch(resetUploadConceptListModal());

  const [isModalOpen, setIsModalOpen] = React.useState(false);
  const [modalContext, setModalContext] = React.useState({});

  const onCloseModal = () => {
    setIsModalOpen(false); // For the Modal "container"
    resetModal(); // For the common UploadConceptListModal
  };

  const onDropFile = async (file, valueIdx, conceptIdx) => {
    setModalContext({ valueIdx, conceptIdx });

    // For the common UploadConceptListModal
    // Wait for file processing before opening the modal
    // => See QueryUploadConceptListModal actions
    await initModal(file);

    setIsModalOpen(true); // For the Modal "container"
  };

  const onAcceptUploadConceptListModal = (
    label,
    rootConcepts,
    resolvedConcepts
  ) => {
    const { valueIdx, conceptIdx } = modalContext;

    props.input.onChange(
      addConceptsFromFile(
        label,
        rootConcepts,
        resolvedConcepts,

        defaults,
        props.isValidConcept,

        props.input.value,
        newValue,

        valueIdx, // From the modal's context
        conceptIdx // From the modal'S context
      )
    );

    onCloseModal();
  };

  const onAcceptCopyModal = (valuesToCopy) => {
    // Deeply copy all values + concepts
    const nextValue = valuesToCopy.reduce((currentValue, value) => {
      const newVal = addValue(currentValue, newValue);

      return value.concepts.reduce(
        (curVal, concept) =>
          addConcept(curVal, curVal.length - 1, copyConcept(concept)),
        newVal
      );
    }, props.input.value);

    return props.input.onChange(nextValue);
  };

  return (
    <div>
      <DropzoneList
        label={
          <>
            {props.label}
            {allowExtendedCopying && (
              <SxTransparentButton
                tiny
                onClick={() => setIsCopyModalOpen(true)}
              >
                {T.translate("externalForms.common.concept.copyFrom")}
              </SxTransparentButton>
            )}
          </>
        }
        dropzoneChildren={({ isOver, itemType }) =>
          isOver && itemType === FORM_CONCEPT_NODE
            ? T.translate("externalForms.common.concept.copying")
            : props.attributeDropzoneText
        }
        acceptedDropTypes={[CONCEPT_TREE_NODE, FORM_CONCEPT_NODE]}
        allowFile={true}
        disallowMultipleColumns={props.disallowMultipleColumns}
        onDelete={(i) =>
          props.input.onChange(removeValue(props.input.value, i))
        }
        onDropFile={(file) => onDropFile(file, props.input.value.length)}
        onDrop={(dropzoneProps, monitor) => {
          const item = monitor.getItem();

          if (item.files) {
            onDropFile(item.files[0], props.input.value.length);

            return;
          }

          if (monitor.getItemType() === FORM_CONCEPT_NODE) {
            return props.input.onChange(
              addConcept(
                addValue(props.input.value, newValue),
                props.input.value.length,
                copyConcept(item.conceptNode)
              )
            );
          }

          if (props.isValidConcept && !props.isValidConcept(item)) return;

          return props.input.onChange(
            addConcept(
              addValue(props.input.value, newValue),
              props.input.value.length, // Assuming the last index has increased after addValue
              initializeConcept(item, defaults)
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
              limit={props.isSingle ? 1 : 0}
              onAddClick={() =>
                props.input.onChange(addConcept(props.input.value, i, null))
              }
              onRemoveClick={(j) =>
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
                    name={props.input.name}
                    hasActiveFilters={nodeHasActiveFilters(concept)}
                    onFilterClick={() =>
                      props.input.onChange(
                        setConceptProperties(props.input.value, i, j, {
                          isEditing: true,
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
                            !concept.includeSubnodes,
                            newValue
                          )
                        ),
                      expandable:
                        !props.isSingle && hasConceptChildren(concept),
                      active: concept.includeSubnodes,
                    }}
                  />
                ) : (
                  <DropzoneWithFileInput
                    acceptedDropTypes={[CONCEPT_TREE_NODE, FORM_CONCEPT_NODE]}
                    onSelectFile={(file) => onDropFile(file, i, j)}
                    onDrop={(_, monitor) => {
                      const item = monitor.getItem();

                      if (item.files) {
                        onDropFile(item.files[0], i, j);

                        return;
                      }

                      if (monitor.getItemType() === FORM_CONCEPT_NODE) {
                        return props.input.onChange(
                          setConcept(
                            props.input.value,
                            i,
                            j,
                            copyConcept(item.conceptNode)
                          )
                        );
                      }

                      if (props.isValidConcept && !props.isValidConcept(item))
                        return null;

                      return props.input.onChange(
                        setConcept(
                          props.input.value,
                          i,
                          j,
                          initializeConcept(item, defaults)
                        )
                      );
                    }}
                  >
                    {({ isOver, itemType }) =>
                      isOver && itemType === FORM_CONCEPT_NODE
                        ? T.translate("externalForms.common.concept.copying")
                        : props.conceptDropzoneText
                    }
                  </DropzoneWithFileInput>
                )
              )}
            />
          </DropzoneListItem>
        ))}
      />
      {isCopyModalOpen && (
        <FormConceptCopyModal
          targetFieldname={props.fieldName}
          onAccept={onAcceptCopyModal}
          onClose={() => setIsCopyModalOpen(false)}
        />
      )}
      {isModalOpen && (
        <UploadConceptListModal
          selectedDatasetId={props.datasetId}
          onAccept={onAcceptUploadConceptListModal}
          onClose={onCloseModal}
        />
      )}
      <FormQueryNodeEditor
        formType={props.formType}
        fieldName={props.input.name}
        datasetId={props.datasetId}
        blacklistedTables={props.blacklistedTables}
        whitelistedTables={props.whitelistedTables}
        onCloseModal={(valueIdx, conceptIdx) =>
          props.input.onChange(
            setConceptProperties(props.input.value, valueIdx, conceptIdx, {
              isEditing: false,
            })
          )
        }
        onUpdateLabel={(valueIdx, conceptIdx, label) =>
          props.input.onChange(
            setConceptProperties(props.input.value, valueIdx, conceptIdx, {
              label,
            })
          )
        }
        onDropConcept={(valueIdx, conceptIdx, concept) => {
          const node = props.input.value[valueIdx].concepts[conceptIdx];

          props.input.onChange(
            setConceptProperties(props.input.value, valueIdx, conceptIdx, {
              ids: [...concept.ids, ...node.ids],
            })
          );
        }}
        onRemoveConcept={(valueIdx, conceptIdx, conceptId) => {
          const node = props.input.value[valueIdx].concepts[conceptIdx];

          props.input.onChange(
            setConceptProperties(props.input.value, valueIdx, conceptIdx, {
              ids: node.ids.filter((id) => id !== conceptId),
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
        onSetDateColumn={(valueIdx, conceptIdx, tableIdx, dateColumnValue) =>
          props.input.onChange(
            setDateColumn(
              props.input.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              dateColumnValue
            )
          )
        }
      />
    </div>
  );
};

export default FormConceptGroup;
