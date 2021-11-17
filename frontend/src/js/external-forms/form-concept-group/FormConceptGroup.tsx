import styled from "@emotion/styled";
import { ReactNode, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import { usePostPrefixForSuggestions } from "../../api/api";
import {
  PostFilterSuggestionsResponseT,
  SelectOptionT,
  SelectorResultType,
} from "../../api/types";
import { TransparentButton } from "../../button/TransparentButton";
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
import type { TreesT } from "../../concept-trees/reducer";
import { nodeHasActiveFilters } from "../../model/node";
import { selectsWithDefaults } from "../../model/select";
import { resetAllFiltersInTables } from "../../model/table";
import { tablesWithDefaults, tableWithDefaults } from "../../model/table";
import type {
  ConceptQueryNodeType,
  DragItemConceptTreeNode,
  FilterWithValueType,
  TableWithFilterValueT,
} from "../../standard-query-editor/types";
import DropzoneWithFileInput, {
  DragItemFile,
} from "../../ui-components/DropzoneWithFileInput";
import type { ModeT } from "../../ui-components/InputRange";
import ToggleButton from "../../ui-components/ToggleButton";
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
import {
  useAllowExtendedCopying,
  useVisibleConceptListFields,
} from "../stateSelectors";
import {
  initSelectsWithDefaults,
  initTablesWithDefaults,
} from "../transformers";

import FormConceptCopyModal from "./FormConceptCopyModal";
import FormConceptNode, { DragItemFormConceptNode } from "./FormConceptNode";

export interface FormConceptGroupT {
  concepts: (ConceptQueryNodeType | null)[];
  connector: string;
}

interface Props {
  formType: string;
  fieldName: string;
  label: string;
  tooltip?: string;
  newValue: FormConceptGroupT;
  isSingle?: boolean;
  optional?: boolean;
  disallowMultipleColumns?: boolean;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  blocklistedSelects?: SelectorResultType[];
  allowlistedSelects?: SelectorResultType[];
  defaults?: ConceptListDefaultsType;
  conceptDropzoneText: string;
  attributeDropzoneText: string;
  isValidConcept?: (item: ConceptQueryNodeType) => boolean;
  value: FormConceptGroupT[];
  onChange: (value: FormConceptGroupT[]) => void;
  renderRowPrefix?: (props: {
    value: FormConceptGroupT[];
    onChange: (value: FormConceptGroupT[]) => void;
    row: FormConceptGroupT;
    i: number;
  }) => ReactNode;
}

const addValue = (value: FormConceptGroupT[], newValue: FormConceptGroupT) => [
  ...value,
  newValue,
];

const removeValue = (value: FormConceptGroupT[], valueIdx: number) => {
  return [...value.slice(0, valueIdx), ...value.slice(valueIdx + 1)];
};

const setValueProperties = (
  value: FormConceptGroupT[],
  valueIdx: number,
  props: Partial<FormConceptGroupT>,
) => {
  return [
    ...value.slice(0, valueIdx),
    {
      ...value[valueIdx],
      ...props,
    },
    ...value.slice(valueIdx + 1),
  ];
};

const addConcept = (
  value: FormConceptGroupT[],
  valueIdx: number,
  item: ConceptQueryNodeType | null,
) =>
  setValueProperties(value, valueIdx, {
    concepts: [...value[valueIdx].concepts, item],
  });

const removeConcept = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
) =>
  setValueProperties(value, valueIdx, {
    concepts: [
      ...value[valueIdx].concepts.slice(0, conceptIdx),
      ...value[valueIdx].concepts.slice(conceptIdx + 1),
    ],
  });

const setConcept = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  item: ConceptQueryNodeType,
) =>
  setValueProperties(value, valueIdx, {
    concepts: [
      ...value[valueIdx].concepts.slice(0, conceptIdx),
      item,
      ...value[valueIdx].concepts.slice(conceptIdx + 1),
    ],
  });

const setConceptProperties = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  props: Partial<ConceptQueryNodeType>,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  return concept
    ? setConcept(value, valueIdx, conceptIdx, {
        ...concept,
        ...props,
      })
    : value;
};

const setTableProperties = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  props: Partial<TableWithFilterValueT>,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  if (!concept) return value;

  const tables = concept.tables;

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
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  props: Partial<FilterWithValueType>,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  if (!concept) return value;

  const filters = concept.tables[tableIdx].filters;

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
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  includeSubnodes: boolean,
  newValue: FormConceptGroupT,
) => {
  const element = value[valueIdx];
  const concept = element.concepts[conceptIdx];
  const conceptData = getConceptById(concept.ids);

  const childIds: string[] = [];
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
  label: string,
  rootConcepts: TreesT,
  resolvedConcepts: string[],
): ConceptQueryNodeType | null => {
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
  label: string,
  rootConcepts: TreesT,
  resolvedConcepts: string[],

  defaults: ConceptListDefaultsType,
  isValidConcept: ((item: ConceptQueryNodeType) => boolean) | undefined,

  value: FormConceptGroupT[],
  newValue: FormConceptGroupT,

  valueIdx: number,
  conceptIdx?: number,
) => {
  const queryElement = createQueryNodeFromConceptListUploadResult(
    label,
    rootConcepts,
    resolvedConcepts,
  );

  if (!queryElement) return value;

  const concept = initializeConcept(queryElement, defaults);

  if (!concept || (!!isValidConcept && !isValidConcept(concept))) return value;

  if (exists(conceptIdx)) {
    return setConcept(value, valueIdx, conceptIdx, concept);
  } else {
    return addConcept(addValue(value, newValue), valueIdx, concept);
  }
};

const initializeConcept = (
  item: ConceptQueryNodeType,
  defaults: ConceptListDefaultsType,
) => {
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
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  isExcluded: boolean,
) => {
  return setTableProperties(value, valueIdx, conceptIdx, tableIdx, {
    exclude: isExcluded,
  });
};

const resetTable = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  if (!concept) return value;

  const table = concept.tables[tableIdx];

  return setTableProperties(
    value,
    valueIdx,
    conceptIdx,
    tableIdx,
    tableWithDefaults(table),
  );
};

const setDateColumn = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  dateColumnValue: string | null,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  return concept
    ? setTableProperties(value, valueIdx, conceptIdx, tableIdx, {
        dateColumn: {
          ...concept.tables[tableIdx].dateColumn,
          value: dateColumnValue || undefined,
        },
      })
    : value;
};

const setFilterValue = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  filterValue: any,
) => {
  return setFilterProperties(value, valueIdx, conceptIdx, tableIdx, filterIdx, {
    value: filterValue,
  });
};

const setSelects = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  selectedSelects: SelectOptionT[],
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  if (!concept) return value;

  const selects = concept.selects;

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
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  selectedSelects: SelectOptionT[],
) => {
  const concept = value[valueIdx].concepts[conceptIdx];
  if (!concept) return value;

  const { tables } = concept;
  const selects = tables[tableIdx].selects;

  if (!selects) return value;

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

const resetAllFilters = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];
  if (!concept) return value;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    tables: resetAllFiltersInTables(concept.tables),
  });
};

const switchFilterMode = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  mode: ModeT,
) => {
  return setFilterProperties(value, valueIdx, conceptIdx, tableIdx, filterIdx, {
    mode: mode,
    value: null,
  });
};

const copyConcept = (item: ConceptQueryNodeType | null) => {
  return JSON.parse(JSON.stringify(item));
};

const updateFilterOptionsWithSuggestions = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  suggestions: PostFilterSuggestionsResponseT["values"],
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
  flex-shrink: 0;
`;

const SxDescription = styled(Description)`
  margin: 0 5px 0 0;
  font-size: ${({ theme }) => theme.font.xs};
`;

interface UploadConceptListModalContext {
  valueIdx: number;
  conceptIdx?: number;
}

const useUploadConceptListModal = ({
  value,
  onChange,
  newValue,
  defaults,
  isValidConcept,
}: {
  value: FormConceptGroupT[];
  onChange: (value: FormConceptGroupT[]) => void;
  newValue: FormConceptGroupT;
  defaults: ConceptListDefaultsType;
  isValidConcept?: (concept: ConceptQueryNodeType) => boolean;
}) => {
  const dispatch = useDispatch();
  const initModal = async (file: File) => {
    const rows = await getUniqueFileRows(file);

    return dispatch(initUploadConceptListModal({ rows, filename: file.name }));
  };
  const resetModal = () => dispatch(resetUploadConceptListModal());

  const [isOpen, setIsOpen] = useState(false);
  const [modalContext, setModalContext] =
    useState<UploadConceptListModalContext | null>(null);

  const onClose = () => {
    setIsOpen(false); // For the Modal "container"
    resetModal(); // For the common UploadConceptListModal
  };

  const onDropFile = async (
    file: File,
    { valueIdx, conceptIdx }: UploadConceptListModalContext,
  ) => {
    setModalContext({ valueIdx, conceptIdx });

    // For the common UploadConceptListModal
    // Wait for file processing before opening the modal
    // => See QueryUploadConceptListModal actions
    await initModal(file);

    setIsOpen(true); // For the Modal "container"
  };

  const onAccept = (
    label: string,
    rootConcepts: TreesT,
    resolvedConcepts: string[],
  ) => {
    if (!modalContext) return;
    const { valueIdx, conceptIdx } = modalContext;

    onChange(
      addConceptsFromFile(
        label,
        rootConcepts,
        resolvedConcepts,

        defaults,
        isValidConcept,

        value,
        newValue,

        valueIdx,
        conceptIdx,
      ),
    );

    onClose();
  };

  return {
    isOpen,
    onClose,
    onDropFile,
    onAccept,
  };
};

const useCopyModal = ({
  value,
  onChange,
  newValue,
}: {
  value: FormConceptGroupT[];
  onChange: (value: FormConceptGroupT[]) => void;
  newValue: FormConceptGroupT;
}) => {
  const [isOpen, setIsOpen] = useState(false);

  const onAccept = (valuesToCopy: FormConceptGroupT[]) => {
    // Deeply copy all values + concepts
    const nextValue = valuesToCopy.reduce((currentValue, value) => {
      const newVal = addValue(currentValue, newValue);

      return value.concepts.reduce(
        (curVal, concept) =>
          addConcept(curVal, curVal.length - 1, copyConcept(concept)),
        newVal,
      );
    }, value);

    return onChange(nextValue);
  };

  return {
    isOpen,
    onAccept,
    setIsOpen,
  };
};

export interface EditedFormQueryNodePosition {
  valueIdx: number;
  conceptIdx: number;
}

const FormConceptGroup = (props: Props) => {
  const { t } = useTranslation();
  const newValue = props.newValue;
  const defaults = props.defaults || {};

  const [editedFormQueryNodePosition, setEditedFormQueryNodePosition] =
    useState<EditedFormQueryNodePosition | null>(null);

  const visibleConceptListFields = useVisibleConceptListFields();
  const allowExtendedCopying = useAllowExtendedCopying(
    props.fieldName,
    visibleConceptListFields,
  );
  const postPrefixForSuggestions = usePostPrefixForSuggestions();

  const {
    isOpen: isUploadConceptListModalOpen,
    onDropFile,
    onAccept: onAcceptUploadConceptListModal,
    onClose: onCloseUploadConceptListModal,
  } = useUploadConceptListModal({
    value: props.value,
    newValue,
    onChange: props.onChange,
    defaults,
    isValidConcept: props.isValidConcept,
  });

  const {
    isOpen: isCopyModalOpen,
    setIsOpen: setIsCopyModalOpen,
    onAccept: onAcceptCopyModal,
  } = useCopyModal({
    value: props.value,
    onChange: props.onChange,
    newValue,
  });

  const editedNode = exists(editedFormQueryNodePosition)
    ? props.value[editedFormQueryNodePosition.valueIdx].concepts[
        editedFormQueryNodePosition.conceptIdx
      ]
    : null;

  return (
    <div>
      <DropzoneList /* TODO: ADD GENERIC TYPE <ConceptQueryNodeType | DragItemFormConceptNode> */
        tooltip={props.tooltip}
        optional={props.optional}
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
        onDelete={(i) => props.onChange(removeValue(props.value, i))}
        onDropFile={(file) =>
          onDropFile(file, { valueIdx: props.value.length })
        }
        onDrop={(
          item:
            | DragItemFile
            | DragItemConceptTreeNode
            | DragItemFormConceptNode,
        ) => {
          if (item.type === "__NATIVE_FILE__") {
            onDropFile(item.files[0], { valueIdx: props.value.length });

            return;
          }

          if (item.type === FORM_CONCEPT_NODE) {
            return props.onChange(
              addConcept(
                addValue(props.value, newValue),
                props.value.length,
                copyConcept(item.conceptNode),
              ),
            );
          }

          if (props.isValidConcept && !props.isValidConcept(item)) return;

          return props.onChange(
            addConcept(
              addValue(props.value, newValue),
              props.value.length, // Assuming the last index has increased after addValue
              initializeConcept(item, defaults),
            ),
          );
        }}
        items={props.value.map((row, i) => (
          <DropzoneListItem>
            {props.renderRowPrefix
              ? props.renderRowPrefix({
                  value: props.value,
                  onChange: props.onChange,
                  row,
                  i,
                })
              : null}
            {row.concepts.length > 1 && (
              <Row>
                <SxDescription>
                  {t("externalForms.common.connectedWith")}:
                </SxDescription>
                <ToggleButton
                  value={props.value[i].connector}
                  onChange={(val) => {
                    props.onChange(
                      setValueProperties(props.value, i, {
                        connector: val,
                      }),
                    );
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
                props.onChange(addConcept(props.value, i, null))
              }
              onRemoveClick={(j) =>
                props.onChange(
                  props.value && props.value[i].concepts.length === 1
                    ? removeValue(props.value, i)
                    : removeConcept(props.value, i, j),
                )
              }
              items={row.concepts.map((concept, j) =>
                concept ? (
                  <FormConceptNode
                    key={j}
                    valueIdx={i}
                    conceptIdx={j}
                    conceptNode={concept}
                    name={props.fieldName}
                    hasActiveFilters={nodeHasActiveFilters(concept)}
                    onFilterClick={() =>
                      setEditedFormQueryNodePosition({
                        valueIdx: i,
                        conceptIdx: j,
                      })
                    }
                    expand={{
                      onClick: () =>
                        props.onChange(
                          onToggleIncludeSubnodes(
                            props.value,
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
                  <DropzoneWithFileInput /* TODO: ADD GENERIC TYPE <ConceptQueryNodeType | DragItemFormConceptNode> */
                    acceptedDropTypes={[CONCEPT_TREE_NODE, FORM_CONCEPT_NODE]}
                    onSelectFile={(file) =>
                      onDropFile(file, { valueIdx: i, conceptIdx: j })
                    }
                    onDrop={(
                      item:
                        | DragItemConceptTreeNode
                        | DragItemFormConceptNode
                        | DragItemFile,
                    ) => {
                      if (item.type === "__NATIVE_FILE__") {
                        onDropFile(item.files[0], {
                          valueIdx: i,
                          conceptIdx: j,
                        });

                        return;
                      }

                      if (item.type === FORM_CONCEPT_NODE) {
                        return props.onChange(
                          setConcept(
                            props.value,
                            i,
                            j,
                            copyConcept(item.conceptNode),
                          ),
                        );
                      }

                      if (props.isValidConcept && !props.isValidConcept(item))
                        return null;

                      return props.onChange(
                        setConcept(
                          props.value,
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
      {isUploadConceptListModalOpen && (
        <UploadConceptListModal
          onAccept={onAcceptUploadConceptListModal}
          onClose={onCloseUploadConceptListModal}
        />
      )}
      {editedFormQueryNodePosition && editedNode && (
        <FormQueryNodeEditor
          formType={props.formType}
          fieldName={props.fieldName}
          node={editedNode}
          nodePosition={editedFormQueryNodePosition}
          blocklistedTables={props.blocklistedTables}
          allowlistedTables={props.allowlistedTables}
          blocklistedSelects={props.blocklistedSelects}
          allowlistedSelects={props.allowlistedSelects}
          onCloseModal={() => setEditedFormQueryNodePosition(null)}
          onUpdateLabel={(label) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              setConceptProperties(props.value, valueIdx, conceptIdx, {
                label,
              }),
            );
          }}
          onDropConcept={(concept) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              setConceptProperties(props.value, valueIdx, conceptIdx, {
                ids: [...concept.ids, ...editedNode.ids],
              }),
            );
          }}
          onRemoveConcept={(conceptId) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              setConceptProperties(props.value, valueIdx, conceptIdx, {
                ids: editedNode.ids.filter((id) => id !== conceptId),
              }),
            );
          }}
          onToggleTable={(tableIdx, isExcluded) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              toggleTable(
                props.value,
                valueIdx,
                conceptIdx,
                tableIdx,
                isExcluded,
              ),
            );
          }}
          onResetTable={(tableIdx) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              resetTable(props.value, valueIdx, conceptIdx, tableIdx),
            );
          }}
          onSelectSelects={(selectedSelects) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              setSelects(props.value, valueIdx, conceptIdx, selectedSelects),
            );
          }}
          onSetFilterValue={(tableIdx, filterIdx, filterValue) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              setFilterValue(
                props.value,
                valueIdx,
                conceptIdx,
                tableIdx,
                filterIdx,
                filterValue,
              ),
            );
          }}
          onSelectTableSelects={(tableIdx, selectedSelects) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              setTableSelects(
                props.value,
                valueIdx,
                conceptIdx,
                tableIdx,
                selectedSelects,
              ),
            );
          }}
          onSwitchFilterMode={(tableIdx, filterIdx, mode) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              switchFilterMode(
                props.value,
                valueIdx,
                conceptIdx,
                tableIdx,
                filterIdx,
                mode,
              ),
            );
          }}
          onResetAllFilters={() => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(resetAllFilters(props.value, valueIdx, conceptIdx));
          }}
          onSetDateColumn={(tableIdx, dateColumnValue) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              setDateColumn(
                props.value,
                valueIdx,
                conceptIdx,
                tableIdx,
                dateColumnValue,
              ),
            );
          }}
          onLoadFilterSuggestions={async (params, tableIdx, filterIdx) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            const { values, total } = await postPrefixForSuggestions(params);

            props.onChange(
              updateFilterOptionsWithSuggestions(
                props.value,
                valueIdx,
                conceptIdx,
                tableIdx,
                filterIdx,
                values,
              ),
            );
          }}
        />
      )}
    </div>
  );
};

export default FormConceptGroup;
