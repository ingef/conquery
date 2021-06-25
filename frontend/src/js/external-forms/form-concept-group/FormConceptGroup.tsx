import styled from "@emotion/styled";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import type { WrappedFieldProps } from "redux-form";

import { usePostPrefixForSuggestions } from "../../api/api";
import { PostFilterSuggestionsResponseT } from "../../api/types";
import TransparentButton from "../../button/TransparentButton";
import {
  CONCEPT_TREE_NODE,
  FORM_CONCEPT_NODE,
} from "../../common/constants/dndTypes";
import { getUniqueFileRows } from "../../common/helpers";
import { compose, includes } from "../../common/helpers/commonHelper";
import { exists } from "../../common/helpers/exists";
import {
  getConceptById,
  getConceptsByIdsWithTablesAndSelects,
  hasConceptChildren,
} from "../../concept-trees/globalTreeStoreHelper";
import DropzoneWithFileInput from "../../form-components/DropzoneWithFileInput";
import ToggleButton from "../../form-components/ToggleButton";
import { nodeHasActiveFilters } from "../../model/node";
import { selectsWithDefaults } from "../../model/select";
import { resetAllFiltersInTables } from "../../model/table";
import { tablesWithDefaults } from "../../model/table";
import type { DragItemConceptTreeNode } from "../../standard-query-editor/types";
import UploadConceptListModal from "../../upload-concept-list-modal/UploadConceptListModal";
import {
  initUploadConceptListModal,
  resetUploadConceptListModal,
} from "../../upload-concept-list-modal/actions";
import type { ConceptListDefaults as ConceptListDefaultsType } from "../config-types";
import { Description } from "../form-components/Description";
import DropzoneList from "../form-components/DropzoneList";
import DynamicInputGroup from "../form-components/DynamicInputGroup";
import FormQueryNodeEditor from "../form-query-node-editor/FormQueryNodeEditor";
import { useAllowExtendedCopying } from "../stateSelectors";
import {
  initSelectsWithDefaults,
  initTablesWithDefaults,
} from "../transformers";

import FormConceptCopyModal from "./FormConceptCopyModal";
import FormConceptNode, { DragItemFormConceptNode } from "./FormConceptNode";

interface PropsType extends WrappedFieldProps {
  formType: string;
  fieldName: string;
  label: string;
  onDropFilterFile: Function;
  newValue?: Object;
  isSingle?: boolean;
  disallowMultipleColumns?: boolean;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  defaults: ConceptListDefaultsType;
  isValidConcept?: Function;
}

const addValue = (value, newValue) => [...value, newValue];

const removeValue = (value, valueIdx: number) => {
  return [...value.slice(0, valueIdx), ...value.slice(valueIdx + 1)];
};

const setValueProperties = (value, valueIdx: number, props) => {
  return [
    ...value.slice(0, valueIdx),
    {
      ...value[valueIdx],
      ...props,
    },
    ...value.slice(valueIdx + 1),
  ];
};

const addConcept = (value, valueIdx, item) =>
  setValueProperties(value, valueIdx, {
    concepts: [...value[valueIdx].concepts, item],
  });

const removeConcept = (value, valueIdx: number, conceptIdx: number) =>
  setValueProperties(value, valueIdx, {
    concepts: [
      ...value[valueIdx].concepts.slice(0, conceptIdx),
      ...value[valueIdx].concepts.slice(conceptIdx + 1),
    ],
  });

const setConcept = (value, valueIdx: number, conceptIdx: number, item) =>
  setValueProperties(value, valueIdx, {
    concepts: [
      ...value[valueIdx].concepts.slice(0, conceptIdx),
      item,
      ...value[valueIdx].concepts.slice(conceptIdx + 1),
    ],
  });

const setConceptProperties = (
  value,
  valueIdx: number,
  conceptIdx: number,
  props,
) =>
  setConcept(value, valueIdx, conceptIdx, {
    ...value[valueIdx].concepts[conceptIdx],
    ...props,
  });

const setTableProperties = (
  value,
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  props,
) => {
  const tables = value[valueIdx].concepts[conceptIdx].tables;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    tables: [
      ...tables.slice(0, tableIdx),
      {
        ...tables[tableIdx],
        ...props,
      },
      ...tables.slice(tableIdx + 1),
    ],
  });
};

const setFilterProperties = (
  value,
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  props,
) => {
  const filters = value[valueIdx].concepts[conceptIdx].tables[tableIdx].filters;

  return setTableProperties(value, valueIdx, conceptIdx, tableIdx, {
    filters: [
      ...filters.slice(0, filterIdx),
      {
        ...filters[filterIdx],
        ...props,
      },
      ...filters.slice(filterIdx + 1),
    ],
  });
};

const onToggleIncludeSubnodes = (
  value,
  valueIdx: number,
  conceptIdx: number,
  includeSubnodes: boolean,
  newValue,
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
      ...element,
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
        val.concepts.filter(exists).some((cpt) => {
          return childIds.every((childId) => !includes(cpt.ids, childId));
        }),
      );

  return setConceptProperties(
    nextValue,
    nextValue.indexOf(element),
    conceptIdx,
    {
      includeSubnodes,
    },
  );
};

const createQueryNodeFromConceptListUploadResult = (
  label,
  rootConcepts,
  resolvedConcepts,
) => {
  const lookupResult = getConceptsByIdsWithTablesAndSelects(
    rootConcepts,
    resolvedConcepts,
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
  conceptIdx = null,
) => {
  const queryElement = createQueryNodeFromConceptListUploadResult(
    label,
    rootConcepts,
    resolvedConcepts,
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
    initTablesWithDefaults(defaults.connectors),
  )({
    ...item,
    tables: tablesWithDefaults(item.tables),
    selects: selectsWithDefaults(item.selects),
  });
};

const toggleTable = (
  value,
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  isExcluded: boolean,
) => {
  return setTableProperties(value, valueIdx, conceptIdx, tableIdx, {
    exclude: isExcluded,
  });
};

const setDateColumn = (
  value,
  valueIdx,
  conceptIdx,
  tableIdx,
  dateColumnValue,
) => {
  return setTableProperties(value, valueIdx, conceptIdx, tableIdx, {
    dateColumn: {
      ...value[valueIdx].concepts[conceptIdx].tables[tableIdx].dateColumn,
      value: dateColumnValue,
    },
  });
};

const setFilterValue = (
  value,
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  filterValue,
) => {
  return setFilterProperties(value, valueIdx, conceptIdx, tableIdx, filterIdx, {
    value: filterValue,
  });
};

const setSelects = (
  value,
  valueIdx: number,
  conceptIdx: number,
  selectedSelects,
) => {
  const concepts = value[valueIdx].concepts;
  const selects = concepts[conceptIdx].selects;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    // value contains the selects that have now been selected
    selects: selects.map((select) => ({
      ...select,
      selected: !selectedSelects
        ? false
        : !!selectedSelects.find(
            (selectedValue) => selectedValue.value === select.id,
          ),
    })),
  });
};

const setTableSelects = (
  value,
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  selectedSelects,
) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;
  const selects = tables[tableIdx].selects;

  return setTableProperties(value, valueIdx, conceptIdx, tableIdx, {
    // value contains the selects that have now been selected
    selects: selects.map((select) => ({
      ...select,
      selected: !selectedSelects
        ? false
        : !!selectedSelects.find(
            (selectedValue) => selectedValue.value === select.id,
          ),
    })),
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
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  mode,
) => {
  return setFilterProperties(value, valueIdx, conceptIdx, tableIdx, filterIdx, {
    mode: mode,
    value: null,
  });
};

const copyConcept = (item) => {
  return JSON.parse(JSON.stringify(item));
};

const updateFilterOptionsWithSuggestions = (
  value,
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  suggestions: PostFilterSuggestionsResponseT,
) => {
  return setFilterProperties(value, valueIdx, conceptIdx, tableIdx, filterIdx, {
    options: suggestions,
  });
};

const DropzoneListItem = styled("div")``;
const Row = styled("div")`
  display: flex;
  align-items: center;
  margin-bottom: 5px;
`;

const SxTransparentButton = styled(TransparentButton)`
  margin-left: 10px;
`;

const SxDescription = styled(Description)`
  margin: 0 5px 0 0;
  font-size: ${({ theme }) => theme.font.xs};
`;

const FormConceptGroup = (props: PropsType) => {
  const newValue = props.newValue;
  const defaults = props.defaults || {};

  const { t } = useTranslation();
  const [isCopyModalOpen, setIsCopyModalOpen] = useState(false);
  const allowExtendedCopying = useAllowExtendedCopying(props.fieldName);
  const postPrefixForSuggestions = usePostPrefixForSuggestions();

  const dispatch = useDispatch();

  const initModal = async (file: File) => {
    const rows = await getUniqueFileRows(file);

    return dispatch(initUploadConceptListModal({ rows, filename: file.name }));
  };
  const resetModal = () => dispatch(resetUploadConceptListModal());

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalContext, setModalContext] = useState({});

  const onCloseModal = () => {
    setIsModalOpen(false); // For the Modal "container"
    resetModal(); // For the common UploadConceptListModal
  };

  const onDropFile = async (
    file: File,
    valueIdx: number,
    conceptIdx: number,
  ) => {
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
    resolvedConcepts,
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
        conceptIdx, // From the modal'S context
      ),
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
        newVal,
      );
    }, props.input.value);

    return props.input.onChange(nextValue);
  };

  return (
    <div>
      <DropzoneList<DragItemConceptTreeNode | DragItemFormConceptNode>
        label={
          <>
            {props.label}
            {allowExtendedCopying && (
              <SxTransparentButton
                tiny
                onClick={() => setIsCopyModalOpen(true)}
              >
                {t("externalForms.common.concept.copyFrom")}
              </SxTransparentButton>
            )}
          </>
        }
        dropzoneChildren={({ isOver, itemType }) =>
          isOver && itemType === FORM_CONCEPT_NODE
            ? t("externalForms.common.concept.copying")
            : props.attributeDropzoneText
        }
        acceptedDropTypes={[CONCEPT_TREE_NODE, FORM_CONCEPT_NODE]}
        disallowMultipleColumns={props.disallowMultipleColumns}
        onDelete={(i) =>
          props.input.onChange(removeValue(props.input.value, i))
        }
        onDropFile={(file) => onDropFile(file, props.input.value.length)}
        onDrop={(item) => {
          if ("files" in item && item.files) {
            onDropFile(item.files[0], props.input.value.length);

            return;
          }

          if (item.type === FORM_CONCEPT_NODE) {
            return props.input.onChange(
              addConcept(
                addValue(props.input.value, newValue),
                props.input.value.length,
                copyConcept(item.conceptNode),
              ),
            );
          }

          if (props.isValidConcept && !props.isValidConcept(item)) return;

          return props.input.onChange(
            addConcept(
              addValue(props.input.value, newValue),
              props.input.value.length, // Assuming the last index has increased after addValue
              initializeConcept(item, defaults),
            ),
          );
        }}
        items={props.input.value.map((row, i) => (
          <DropzoneListItem>
            {props.renderRowPrefix
              ? props.renderRowPrefix(props.input, row, i)
              : null}
            {row.concepts.length > 1 && (
              <Row>
                <SxDescription>
                  {t("externalForms.common.connectedWith")}:
                </SxDescription>
                <ToggleButton
                  input={{
                    value: props.input.value[i].connector,
                    onChange: (value) => {
                      props.input.onChange(
                        setValueProperties(props.input.value, i, {
                          connector: value,
                        }),
                      );
                    },
                  }}
                  options={[
                    { value: "OR", label: t("common.or") },
                    { value: "AND", label: t("common.and") },
                  ]}
                />
              </Row>
            )}
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
                    : removeConcept(props.input.value, i, j),
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
                        }),
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
                            newValue,
                          ),
                        ),
                      expandable:
                        !props.isSingle && hasConceptChildren(concept),
                      active: concept.includeSubnodes,
                    }}
                  />
                ) : (
                  <DropzoneWithFileInput<
                    DragItemConceptTreeNode | DragItemFormConceptNode
                  >
                    acceptedDropTypes={[CONCEPT_TREE_NODE, FORM_CONCEPT_NODE]}
                    onSelectFile={(file) => onDropFile(file, i, j)}
                    onDrop={(item) => {
                      if ("files" in item && item.files) {
                        onDropFile(item.files[0], i, j);

                        return;
                      }

                      if (item.type === FORM_CONCEPT_NODE) {
                        return props.input.onChange(
                          setConcept(
                            props.input.value,
                            i,
                            j,
                            copyConcept(item.conceptNode),
                          ),
                        );
                      }

                      if (props.isValidConcept && !props.isValidConcept(item))
                        return null;

                      return props.input.onChange(
                        setConcept(
                          props.input.value,
                          i,
                          j,
                          initializeConcept(item, defaults),
                        ),
                      );
                    }}
                  >
                    {({ isOver, itemType }) =>
                      isOver && itemType === FORM_CONCEPT_NODE
                        ? t("externalForms.common.concept.copying")
                        : props.conceptDropzoneText
                    }
                  </DropzoneWithFileInput>
                ),
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
          onAccept={onAcceptUploadConceptListModal}
          onClose={onCloseModal}
        />
      )}
      <FormQueryNodeEditor
        formType={props.formType}
        fieldName={props.input.name}
        blocklistedTables={props.blocklistedTables}
        allowlistedTables={props.allowlistedTables}
        onCloseModal={(valueIdx, conceptIdx) =>
          props.input.onChange(
            setConceptProperties(props.input.value, valueIdx, conceptIdx, {
              isEditing: false,
            }),
          )
        }
        onUpdateLabel={(valueIdx, conceptIdx, label) =>
          props.input.onChange(
            setConceptProperties(props.input.value, valueIdx, conceptIdx, {
              label,
            }),
          )
        }
        onDropConcept={(valueIdx, conceptIdx, concept) => {
          const node = props.input.value[valueIdx].concepts[conceptIdx];

          props.input.onChange(
            setConceptProperties(props.input.value, valueIdx, conceptIdx, {
              ids: [...concept.ids, ...node.ids],
            }),
          );
        }}
        onRemoveConcept={(valueIdx, conceptIdx, conceptId) => {
          const node = props.input.value[valueIdx].concepts[conceptIdx];

          props.input.onChange(
            setConceptProperties(props.input.value, valueIdx, conceptIdx, {
              ids: node.ids.filter((id) => id !== conceptId),
            }),
          );
        }}
        onToggleTable={(valueIdx, conceptIdx, tableIdx, isExcluded) =>
          props.input.onChange(
            toggleTable(
              props.input.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              isExcluded,
            ),
          )
        }
        onSelectSelects={(valueIdx, conceptIdx, selectedSelects) =>
          props.input.onChange(
            setSelects(
              props.input.value,
              valueIdx,
              conceptIdx,
              selectedSelects,
            ),
          )
        }
        onSetFilterValue={(
          valueIdx,
          conceptIdx,
          tableIdx,
          filterIdx,
          filterValue,
        ) =>
          props.input.onChange(
            setFilterValue(
              props.input.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              filterIdx,
              filterValue,
            ),
          )
        }
        onSelectTableSelects={(
          valueIdx,
          conceptIdx,
          tableIdx,
          selectedSelects,
        ) =>
          props.input.onChange(
            setTableSelects(
              props.input.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              selectedSelects,
            ),
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
              mode,
            ),
          )
        }
        onResetAllFilters={(valueIdx, conceptIdx) =>
          props.input.onChange(
            resetAllFilters(props.input.value, valueIdx, conceptIdx),
          )
        }
        onSetDateColumn={(valueIdx, conceptIdx, tableIdx, dateColumnValue) =>
          props.input.onChange(
            setDateColumn(
              props.input.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              dateColumnValue,
            ),
          )
        }
        onLoadFilterSuggestions={async (
          valueIdx,
          conceptIdx,
          params,
          tableIdx,
          filterIdx,
        ) => {
          const suggestions = await postPrefixForSuggestions(params);

          props.input.onChange(
            updateFilterOptionsWithSuggestions(
              props.input.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              filterIdx,
              suggestions,
            ),
          );
        }}
      />
    </div>
  );
};

export default FormConceptGroup;
