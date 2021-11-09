import styled from "@emotion/styled";
import { TreesT } from "js/concept-trees/reducer";
import { useState } from "react";
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
import { nodeHasActiveFilters } from "../../model/node";
import { selectsWithDefaults } from "../../model/select";
import { resetAllFiltersInTables } from "../../model/table";
import { tablesWithDefaults } from "../../model/table";
import type {
  ConceptQueryNodeType,
  FilterWithValueType,
  TableWithFilterValueT,
} from "../../standard-query-editor/types";
import DropzoneWithFileInput from "../../ui-components/DropzoneWithFileInput";
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
import { useAllowExtendedCopying } from "../stateSelectors";
import {
  initSelectsWithDefaults,
  initTablesWithDefaults,
} from "../transformers";

import FormConceptCopyModal from "./FormConceptCopyModal";
import FormConceptNode, { DragItemFormConceptNode } from "./FormConceptNode";

export interface FormConceptGroupT {
  concepts: ConceptQueryNodeType[];
  connector: string;
}

interface Props {
  formType: string;
  fieldName: string;
  label: string;
  tooltip?: string;
  onDropFilterFile: Function;
  newValue: FormConceptGroupT;
  isSingle?: boolean;
  optional?: boolean;
  disallowMultipleColumns?: boolean;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  blocklistedSelects?: SelectorResultType[];
  allowlistedSelects?: SelectorResultType[];
  defaults: ConceptListDefaultsType;
  isValidConcept?: (item: ConceptQueryNodeType) => boolean;
  value: FormConceptGroupT[];
  onChange: (value: FormConceptGroupT[]) => void;
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
  item: ConceptQueryNodeType,
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
) =>
  setConcept(value, valueIdx, conceptIdx, {
    ...value[valueIdx].concepts[conceptIdx],
    ...props,
  });

const setTableProperties = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  props: Partial<TableWithFilterValueT>,
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
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  props: Partial<FilterWithValueType>,
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
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  includeSubnodes: boolean,
  newValue: FormConceptGroupT,
) => {
  const element = value[valueIdx];
  const concept = element.concepts[conceptIdx];
  console.log(concept.ids);
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

const setDateColumn = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  dateColumnValue: string | null,
) => {
  return setTableProperties(value, valueIdx, conceptIdx, tableIdx, {
    dateColumn: {
      ...value[valueIdx].concepts[conceptIdx].tables[tableIdx].dateColumn,
      value: dateColumnValue || undefined,
    },
  });
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
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  selectedSelects: SelectOptionT[],
) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;
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
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    tables: resetAllFiltersInTables(tables),
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

const copyConcept = (item: ConceptQueryNodeType) => {
  return JSON.parse(JSON.stringify(item));
};

const updateFilterOptionsWithSuggestions = (
  value: FormConceptGroupT[],
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

const FormConceptGroup = (props: Props) => {
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
  const [modalContext, setModalContext] =
    useState<UploadConceptListModalContext | null>(null);

  const onCloseModal = () => {
    setIsModalOpen(false); // For the Modal "container"
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

    setIsModalOpen(true); // For the Modal "container"
  };

  const onAcceptUploadConceptListModal = (
    label: string,
    rootConcepts: TreesT,
    resolvedConcepts: string[],
  ) => {
    if (!modalContext) return;
    const { valueIdx, conceptIdx } = modalContext;

    props.onChange(
      addConceptsFromFile(
        label,
        rootConcepts,
        resolvedConcepts,

        defaults,
        props.isValidConcept,

        props.value,
        newValue,

        valueIdx,
        conceptIdx,
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
    }, props.value);

    return props.onChange(nextValue);
  };

  return (
    <div>
      <DropzoneList<ConceptQueryNodeType | DragItemFormConceptNode>
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
        onDrop={(item) => {
          if ("files" in item && item.files) {
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
              ? props.renderRowPrefix(props.input, row, i)
              : null}
            {row.concepts.length > 1 && (
              <Row>
                <SxDescription>
                  {t("externalForms.common.connectedWith")}:
                </SxDescription>
                <ToggleButton
                  input={{
                    value: props.value[i].connector,
                    onChange: (value) => {
                      props.onChange(
                        setValueProperties(props.value, i, {
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
                    name={props.name}
                    hasActiveFilters={nodeHasActiveFilters(concept)}
                    onFilterClick={() =>
                      props.onChange(
                        setConceptProperties(props.value, i, j, {
                          isEditing: true,
                        }),
                      )
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
                  <DropzoneWithFileInput<
                    ConceptQueryNodeType | DragItemFormConceptNode
                  >
                    acceptedDropTypes={[CONCEPT_TREE_NODE, FORM_CONCEPT_NODE]}
                    onSelectFile={(file) =>
                      onDropFile(file, { valueIdx: i, conceptIdx: j })
                    }
                    onDrop={(item) => {
                      if ("files" in item && item.files) {
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
      {isModalOpen && (
        <UploadConceptListModal
          onAccept={onAcceptUploadConceptListModal}
          onClose={onCloseModal}
        />
      )}
      <FormQueryNodeEditor
        formType={props.formType}
        fieldName={props.fieldName}
        blocklistedTables={props.blocklistedTables}
        allowlistedTables={props.allowlistedTables}
        blocklistedSelects={props.blocklistedSelects}
        allowlistedSelects={props.allowlistedSelects}
        onCloseModal={(valueIdx, conceptIdx) =>
          props.onChange(
            setConceptProperties(props.value, valueIdx, conceptIdx, {
              isEditing: false,
            }),
          )
        }
        onUpdateLabel={(valueIdx, conceptIdx, label) =>
          props.onChange(
            setConceptProperties(props.value, valueIdx, conceptIdx, {
              label,
            }),
          )
        }
        onDropConcept={(valueIdx, conceptIdx, concept) => {
          const node = props.value[valueIdx].concepts[conceptIdx];

          props.onChange(
            setConceptProperties(props.value, valueIdx, conceptIdx, {
              ids: [...concept.ids, ...node.ids],
            }),
          );
        }}
        onRemoveConcept={(valueIdx, conceptIdx, conceptId) => {
          const node = props.value[valueIdx].concepts[conceptIdx];

          props.onChange(
            setConceptProperties(props.value, valueIdx, conceptIdx, {
              ids: node.ids.filter((id) => id !== conceptId),
            }),
          );
        }}
        onToggleTable={(valueIdx, conceptIdx, tableIdx, isExcluded) =>
          props.onChange(
            toggleTable(
              props.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              isExcluded,
            ),
          )
        }
        onSelectSelects={(valueIdx, conceptIdx, selectedSelects) =>
          props.onChange(
            setSelects(props.value, valueIdx, conceptIdx, selectedSelects),
          )
        }
        onSetFilterValue={(
          valueIdx,
          conceptIdx,
          tableIdx,
          filterIdx,
          filterValue,
        ) =>
          props.onChange(
            setFilterValue(
              props.value,
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
          props.onChange(
            setTableSelects(
              props.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              selectedSelects,
            ),
          )
        }
        onSwitchFilterMode={(valueIdx, conceptIdx, tableIdx, filterIdx, mode) =>
          props.onChange(
            switchFilterMode(
              props.value,
              valueIdx,
              conceptIdx,
              tableIdx,
              filterIdx,
              mode,
            ),
          )
        }
        onResetAllFilters={(valueIdx, conceptIdx) =>
          props.onChange(resetAllFilters(props.value, valueIdx, conceptIdx))
        }
        onSetDateColumn={(valueIdx, conceptIdx, tableIdx, dateColumnValue) =>
          props.onChange(
            setDateColumn(
              props.value,
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

          props.onChange(
            updateFilterOptionsWithSuggestions(
              props.value,
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
