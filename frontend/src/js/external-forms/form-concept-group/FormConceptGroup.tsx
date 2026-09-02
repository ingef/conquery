import { type ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { usePostPrefixForSuggestions } from "../../api/api";
import type { SelectorResultType } from "../../api/types";
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
  isMovedObject,
  type PossibleDroppableObject,
} from "../../ui-components/Dropzone";
import DropzoneWithFileInput, {
  type DragItemFile,
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
  addConcept,
  addValue,
  copyConcept,
  type FormConceptGroupT,
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

// named to avoid shadowing the `row` map param below
const connectorRow = tv({
  base: ["flex items-center", "mb-[5px]"],
});

// Description's own margins are overridden here
const connectorDescription = tv({
  base: ["m-0 mr-[5px]", "text-xs"],
});

export interface EditedFormQueryNodePosition {
  valueIdx: number;
  conceptIdx: number;
}

const DROP_TYPES = [DNDType.CONCEPT_TREE_NODE];

interface ConceptContext {
  defaults: ConceptListDefaultsType;
  tableConfig: Parameters<typeof initializeConcept>[2];
  selectConfig: Parameters<typeof initializeConcept>[3];
}

const toConcept = (item: DragItemConceptTreeNode, ctx: ConceptContext) =>
  isMovedObject(item)
    ? copyConcept(item)
    : initializeConcept(item, ctx.defaults, ctx.tableConfig, ctx.selectConfig);

const cloneNewValue = (props: Props) =>
  JSON.parse(JSON.stringify(props.newValue));

const removeConceptOrRow = (props: Props, i: number, j: number) =>
  props.value[i].concepts.length === 1
    ? removeValue(props.value, i)
    : removeConcept(props.value, i, j);

// A concept dropped between two rows opens a new row there. If it was moved from
// within the same field, its old slot is removed first, which may shift the index.
const dropConceptBetweenRows = (
  props: Props,
  ctx: ConceptContext,
  i: number,
  item: PossibleDroppableObject,
) => {
  if (item.type !== DNDType.CONCEPT_TREE_NODE) return null;
  if (props.isValidConcept && !props.isValidConcept(item)) return null;

  const concept = toConcept(item, ctx);

  let insertIndex = i;
  let newPropsValue = props.value;
  const newValue = cloneNewValue(props);

  if (isMovedObject(item)) {
    const { movedFromFieldName, movedFromAndIdx, movedFromOrIdx } =
      item.dragContext;

    if (movedFromFieldName === props.fieldName) {
      const movedConceptWasLast =
        props.value[movedFromAndIdx].concepts.length === 1;
      const willConceptMoveDown = i > movedFromAndIdx && movedConceptWasLast;

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
    } else if (exists(item.dragContext.deleteFromOtherField)) {
      item.dragContext.deleteFromOtherField();
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

// A concept dropped on the group's own dropzone gets a new row at the end
const dropConceptOnGroup = (
  props: Props,
  ctx: ConceptContext,
  item: DragItemConceptTreeNode,
) => {
  if (props.isValidConcept && !props.isValidConcept(item)) return;

  const newValue = cloneNewValue(props);

  // rowPrefixField is a special property that is only used in an edge case form,
  // for a detailed explanation see dropConceptBetweenRows
  if (isMovedObject(item)) {
    const { movedFromFieldName, movedFromAndIdx } = item.dragContext;

    if (movedFromFieldName === props.fieldName && props.rowPrefixFieldname) {
      newValue[props.rowPrefixFieldname] =
        // @ts-ignore rowPrefixFieldname is dynamic, and since it's an edge case, we're not typing this
        props.value[movedFromAndIdx][props.rowPrefixFieldname];
    }
  }

  return props.onChange(
    addConcept(
      addValue(props.value, newValue),
      props.value.length, // Assuming the last index has increased after addValue
      toConcept(item, ctx),
    ),
  );
};

// A concept dropped on an empty slot inside a row fills that slot
const dropConceptOnSlot = (
  props: Props,
  ctx: ConceptContext,
  i: number,
  j: number,
  item: DragItemConceptTreeNode,
) => {
  if (props.isValidConcept && !props.isValidConcept(item)) return null;

  return props.onChange(setConcept(props.value, i, j, toConcept(item, ctx)));
};

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
  const conceptContext: ConceptContext = {
    defaults,
    tableConfig,
    selectConfig,
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
              <TransparentButton
                className="ml-[10px] shrink-0"
                tiny
                onClick={() => setIsCopyModalOpen(true)}
              >
                {t("externalForms.common.concept.copyFrom")}
              </TransparentButton>
            )}
          </>
        }
        dropzoneChildren={({ isOver, item }) =>
          isOver && isMovedObject(item)
            ? t("externalForms.common.concept.copying")
            : props.attributeDropzoneText
        }
        dropBetween={(i: number) => (item: PossibleDroppableObject) =>
          dropConceptBetweenRows(props, conceptContext, i, item)
        }
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

          return dropConceptOnGroup(props, conceptContext, item);
        }}
        items={props.value.map((row, i) => (
          <div key={i}>
            {props.renderRowPrefix
              ? props.renderRowPrefix({
                  value: props.value,
                  onChange: props.onChange,
                  row,
                  i,
                })
              : null}
            {row.concepts.length > 1 && (
              <div className={connectorRow()}>
                <Description className={connectorDescription()}>
                  {t("externalForms.common.connectedWith")}:
                </Description>
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
              </div>
            )}
            <DynamicInputGroup
              key={i}
              limit={props.isSingle ? 1 : 0}
              onAddClick={() =>
                props.onChange(addConcept(props.value, i, null))
              }
              onRemoveClick={(j) =>
                props.onChange(removeConceptOrRow(props, i, j))
              }
              items={row.concepts.map((concept, j) =>
                concept ? (
                  <FormConceptNode
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
                    deleteFromOtherField={() =>
                      props.onChange(removeConceptOrRow(props, i, j))
                    }
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
                    key={j}
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

                      return dropConceptOnSlot(
                        props,
                        conceptContext,
                        i,
                        j,
                        item,
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
          </div>
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
