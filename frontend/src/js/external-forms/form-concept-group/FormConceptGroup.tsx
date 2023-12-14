import styled from "@emotion/styled";
import { ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";

import { usePostPrefixForSuggestions } from "../../api/api";
import { SelectorResultType } from "../../api/types";
import { TransparentButton } from "../../button/TransparentButton";
import { DNDType } from "../../common/constants/dndTypes";
import { exists } from "../../common/helpers/exists";
import {
  getConceptById,
  hasConceptChildren,
} from "../../concept-trees/globalTreeStoreHelper";
import {
  nodeHasFilterValues,
  nodeHasNonDefaultSettings,
} from "../../model/node";
import type { DragItemConceptTreeNode } from "../../standard-query-editor/types";
import {
  PossibleDroppableObject,
  isMovedObject,
} from "../../ui-components/Dropzone";
import DropzoneWithFileInput, {
  DragItemFile,
} from "../../ui-components/DropzoneWithFileInput";
import ToggleButton from "../../ui-components/ToggleButton";
import UploadConceptListModal from "../../upload-concept-list-modal/UploadConceptListModal";
import type { ConceptListDefaults as ConceptListDefaultsType } from "../config-types";
import { Description } from "../form-components/Description";
import DropzoneList from "../form-components/DropzoneList";
import DynamicInputGroup from "../form-components/DynamicInputGroup";
import FormQueryNodeEditor from "../form-query-node-editor/FormQueryNodeEditor";
import {
  useAllowExtendedCopying,
  useVisibleConceptListFields,
} from "../stateSelectors";

import FormConceptCopyModal from "./FormConceptCopyModal";
import FormConceptNode from "./FormConceptNode";
import {
  FormConceptGroupT,
  addConcept,
  addValue,
  copyConcept,
  initializeConcept,
  insertValue,
  onToggleIncludeSubnodes,
  removeConcept,
  removeValue,
  resetAllSettings,
  resetTable,
  setConcept,
  setConceptProperties,
  setDateColumn,
  setFilterValue,
  setSelects,
  setTableSelects,
  setValueProperties,
  switchFilterMode,
  toggleTable,
  updateFilterOptionsWithSuggestions,
} from "./formConceptGroupState";
import { useCopyModal } from "./useCopyModal";
import { useUploadConceptListModal } from "./useUploadConceptListModal";

interface Props {
  formType: string;
  fieldName: string;
  label: string;
  tooltip?: string;
  newValue: FormConceptGroupT;
  isSingle?: boolean;
  disallowMultipleColumns?: boolean;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  blocklistedSelects?: SelectorResultType[];
  allowlistedSelects?: SelectorResultType[];
  defaults?: ConceptListDefaultsType;
  conceptDropzoneText: string;
  attributeDropzoneText: string;
  isValidConcept?: (item: DragItemConceptTreeNode) => boolean;
  value: FormConceptGroupT[];
  onChange: (value: FormConceptGroupT[]) => void;
  renderRowPrefix?: (props: {
    value: FormConceptGroupT[];
    onChange: (value: FormConceptGroupT[]) => void;
    row: FormConceptGroupT;
    i: number;
  }) => ReactNode;
  rowPrefixFieldname?: string;
}

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

const SxFormConceptNode = styled(FormConceptNode)`
  margin-top: 5px;
`;

export interface EditedFormQueryNodePosition {
  valueIdx: number;
  conceptIdx: number;
}

const DROP_TYPES = [DNDType.CONCEPT_TREE_NODE];

const FormConceptGroup = (props: Props) => {
  const { t } = useTranslation();
  const newValue = props.newValue;
  const defaults = props.defaults || {};
  const tableConfig = {
    allowlistedTables: props.allowlistedTables,
    blocklistedTables: props.blocklistedTables,
  };
  const selectConfig = {
    allowlistedSelects: props.allowlistedSelects,
    blocklistedSelects: props.blocklistedSelects,
  };

  // indicator if it should be scrolled down back to the dropZone
  const [scrollToDropzone, setScrollToDropzone] = useState<boolean>(false);
  const dropzoneRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (scrollToDropzone) {
      dropzoneRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "nearest",
      });
      setScrollToDropzone(false);
    }
  }, [scrollToDropzone]);

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
    onAcceptConceptsOrFilter: onAcceptUploadModalConceptsOrFilter,
    onClose: onCloseUploadConceptListModal,
    onImportLines,
  } = useUploadConceptListModal({
    value: props.value,
    newValue,
    onChange: props.onChange,
    defaults,
    tableConfig,
    selectConfig,
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

  const editedNode = useMemo(() => {
    return exists(editedFormQueryNodePosition)
      ? props.value[editedFormQueryNodePosition.valueIdx].concepts[
          editedFormQueryNodePosition.conceptIdx
        ]
      : null;
  }, [editedFormQueryNodePosition, props.value]);

  return (
    <div>
      <DropzoneList /* TODO: ADD GENERIC TYPE <ConceptQueryNodeType> */
        ref={dropzoneRef}
        tooltip={props.tooltip}
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
        dropzoneChildren={({ isOver, item }) =>
          isOver && isMovedObject(item)
            ? t("externalForms.common.concept.copying")
            : props.attributeDropzoneText
        }
        dropBetween={(i: number) => {
          return (item: PossibleDroppableObject) => {
            if (item.type !== DNDType.CONCEPT_TREE_NODE) return null;

            if (props.isValidConcept && !props.isValidConcept(item))
              return null;

            const concept = isMovedObject(item)
              ? copyConcept(item)
              : initializeConcept(item, defaults, tableConfig, selectConfig);

            let insertIndex = i;
            let newPropsValue = props.value;
            const newValue = JSON.parse(JSON.stringify(props.newValue));

            if (isMovedObject(item)) {
              const { movedFromFieldName, movedFromAndIdx, movedFromOrIdx } =
                item.dragContext;

              if (movedFromFieldName === props.fieldName) {
                const movedConceptWasLast =
                  props.value[movedFromAndIdx].concepts.length === 1;
                const willConceptMoveDown =
                  i > movedFromAndIdx && movedConceptWasLast;

                if (willConceptMoveDown) {
                  insertIndex = i - 1;
                }
                newPropsValue = movedConceptWasLast
                  ? removeValue(props.value, movedFromAndIdx)
                  : removeConcept(props.value, movedFromAndIdx, movedFromOrIdx);

                // rowPrefixField is a special property that is only used in an edge case form,
                // used for tagging concepts. We only need to pass it back into the value
                // if the concept is moved to a different position in the same field.
                if (props.rowPrefixFieldname) {
                  newValue[props.rowPrefixFieldname] =
                    // @ts-ignore rowPrefixFieldname is dynamic, and since it's an edge case, we're not typing this
                    props.value[movedFromAndIdx][props.rowPrefixFieldname];
                }
              } else {
                if (exists(item.dragContext.deleteFromOtherField)) {
                  item.dragContext.deleteFromOtherField();
                }
              }
            }

            return props.onChange(
              addConcept(
                insertValue(newPropsValue, insertIndex, newValue),
                insertIndex,
                concept,
              ),
            );
          };
        }}
        acceptedDropTypes={[DNDType.CONCEPT_TREE_NODE]}
        disallowMultipleColumns={props.disallowMultipleColumns}
        onDelete={(i) => props.onChange(removeValue(props.value, i))}
        onDropFile={(file) =>
          onDropFile(file, { valueIdx: props.value.length })
        }
        onImportLines={(lines, filename) =>
          onImportLines({ lines, filename }, { valueIdx: props.value.length })
        }
        onDrop={(item: DragItemFile | DragItemConceptTreeNode) => {
          setScrollToDropzone(true);
          if (item.type === "__NATIVE_FILE__") {
            onDropFile(item.files[0], { valueIdx: props.value.length });

            return;
          }

          if (props.isValidConcept && !props.isValidConcept(item)) return;

          const newValue = JSON.parse(JSON.stringify(props.newValue));

          // rowPrefixField is a special property that is only used in an edge case form,
          // for a detailed explanation see the comment in the dropBetween function
          if (isMovedObject(item)) {
            const { movedFromFieldName, movedFromAndIdx } = item.dragContext;

            if (
              movedFromFieldName === props.fieldName &&
              props.rowPrefixFieldname
            ) {
              newValue[props.rowPrefixFieldname] =
                // @ts-ignore rowPrefixFieldname is dynamic, and since it's an edge case, we're not typing this
                props.value[movedFromAndIdx][props.rowPrefixFieldname];
            }
          }

          const concept = isMovedObject(item)
            ? copyConcept(item)
            : initializeConcept(item, defaults, tableConfig, selectConfig);
          return props.onChange(
            addConcept(
              addValue(props.value, newValue),
              props.value.length, // Assuming the last index has increased after addValue
              concept,
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
                  <SxFormConceptNode
                    key={j}
                    valueIdx={i}
                    conceptIdx={j}
                    conceptNode={concept}
                    name={props.fieldName}
                    hasNonDefaultSettings={
                      concept.includeSubnodes ||
                      nodeHasNonDefaultSettings(concept)
                    }
                    hasFilterValues={nodeHasFilterValues(concept)}
                    onClick={() =>
                      setEditedFormQueryNodePosition({
                        valueIdx: i,
                        conceptIdx: j,
                      })
                    }
                    fieldName={props.fieldName}
                    deleteFromOtherField={() => {
                      return props.onChange(
                        props.value[i].concepts.length === 1
                          ? removeValue(props.value, i)
                          : removeConcept(props.value, i, j),
                      );
                    }}
                    // row_prefix is a special property that is only used in an edge case form.
                    // To support reordering of concepts this property needs
                    // to be passed to the concept node
                    rowPrefixFieldname={props.rowPrefixFieldname}
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
                        !props.disallowMultipleColumns &&
                        hasConceptChildren(concept),
                      active: !!concept.includeSubnodes,
                    }}
                  />
                ) : (
                  <DropzoneWithFileInput /* TODO: ADD GENERIC TYPE <DragItemConceptTreeNode> */
                    acceptedDropTypes={DROP_TYPES}
                    onImportLines={(lines, filename) =>
                      onImportLines(
                        { lines, filename },
                        { valueIdx: i, conceptIdx: j },
                      )
                    }
                    onDrop={(item: DragItemConceptTreeNode | DragItemFile) => {
                      if (item.type === "__NATIVE_FILE__") {
                        onDropFile(item.files[0], {
                          valueIdx: i,
                          conceptIdx: j,
                        });

                        return;
                      }

                      if (props.isValidConcept && !props.isValidConcept(item))
                        return null;

                      if (isMovedObject(item)) {
                        return props.onChange(
                          setConcept(props.value, i, j, copyConcept(item)),
                        );
                      }

                      return props.onChange(
                        setConcept(
                          props.value,
                          i,
                          j,
                          initializeConcept(
                            item,
                            defaults,
                            tableConfig,
                            selectConfig,
                          ),
                        ),
                      );
                    }}
                  >
                    {({ isOver, item }) =>
                      isOver && isMovedObject(item)
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
          onAcceptConceptsOrFilter={onAcceptUploadModalConceptsOrFilter}
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
            let { valueIdx } = editedFormQueryNodePosition;
            const { conceptIdx } = editedFormQueryNodePosition;
            let updatedValue = props.value;
            if (isMovedObject(concept)) {
              const { movedFromFieldName, movedFromAndIdx, movedFromOrIdx } =
                concept.dragContext;

              // If the concept is moved from the same field and the concept is the only one
              // in the value the index of the selected concept might change after the drop
              const willSelectedConceptIndexChange =
                valueIdx > movedFromAndIdx &&
                props.value[movedFromOrIdx].concepts.length === 1;
              valueIdx = willSelectedConceptIndexChange
                ? valueIdx - 1
                : valueIdx;
              if (movedFromFieldName === props.fieldName) {
                updatedValue =
                  updatedValue[movedFromAndIdx].concepts.length === 1
                    ? removeValue(updatedValue, movedFromAndIdx)
                    : removeConcept(
                        updatedValue,
                        movedFromAndIdx,
                        movedFromOrIdx,
                      );
                setEditedFormQueryNodePosition({ valueIdx, conceptIdx });
              } else {
                if (exists(concept.dragContext.deleteFromOtherField)) {
                  concept.dragContext.deleteFromOtherField();
                }
              }
            }
            props.onChange(
              setConceptProperties(updatedValue, valueIdx, conceptIdx, {
                ids: [...concept.ids, ...editedNode.ids],
              }),
            );
          }}
          onRemoveConcept={(conceptId) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            const newIds = editedNode.ids.filter((id) => id !== conceptId);
            props.onChange(
              setConceptProperties(props.value, valueIdx, conceptIdx, {
                ids: newIds,
                description:
                  newIds.length === 1
                    ? getConceptById(newIds[0])?.description
                    : editedNode.description,
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
          onResetAllSettings={(config) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              resetAllSettings(props.value, valueIdx, conceptIdx, config),
            );
          }}
          onResetTable={(tableIdx, config) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              resetTable(props.value, valueIdx, conceptIdx, tableIdx, config),
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
          onLoadFilterSuggestions={async (
            params,
            tableIdx,
            filterIdx,
            { returnOnly } = {},
          ) => {
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            const data = await postPrefixForSuggestions(params);

            if (!returnOnly) {
              props.onChange(
                updateFilterOptionsWithSuggestions(
                  props.value,
                  valueIdx,
                  conceptIdx,
                  tableIdx,
                  filterIdx,
                  data,
                  params.page,
                ),
              );
            }

            return data;
          }}
        />
      )}
    </div>
  );
};

export default FormConceptGroup;
