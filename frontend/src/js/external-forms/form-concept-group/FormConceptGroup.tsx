import styled from "@emotion/styled";
import { ReactNode, useEffect, useState, useRef, useMemo } from "react";
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
import { isMovedObject } from "../../ui-components/Dropzone";
import DropzoneWithFileInput, {
  DragItemFile,
} from "../../ui-components/DropzoneWithFileInput";
import ToggleButton from "../../ui-components/ToggleButton";
import UploadConceptListModal from "../../upload-concept-list-modal/UploadConceptListModal";
import type { ConceptListDefaults as ConceptListDefaultsType } from "../config-types";
import { Description } from "../form-components/Description";
import DropzoneBetweenElements from "../form-components/DropzoneBetweenElements";
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
  FormConceptGroupT,
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
  optional?: boolean;
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
}

const DropzoneListItem = styled("div")`
  margin-top: -30px;
`;
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
        dropzoneChildren={({ isOver, item }) =>
          isOver && isMovedObject(item)
            ? t("externalForms.common.concept.copying")
            : props.attributeDropzoneText
        }
        dropBetween={(i: number) => {
          return (item: DragItemConceptTreeNode) => {
            if (isMovedObject(item)) {
              return props.onChange(
                addConcept(
                  insertValue(props.value, i, newValue),
                  i,
                  copyConcept(item),
                ),
              );
            }

            if (props.isValidConcept && !props.isValidConcept(item))
              return null;

            return props.onChange(
              addConcept(
                insertValue(props.value, i, newValue),
                i,
                initializeConcept(item, defaults, tableConfig),
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
        onImportLines={(lines) =>
          onImportLines(lines, { valueIdx: props.value.length })
        }
        onDrop={(item: DragItemFile | DragItemConceptTreeNode) => {
          setScrollToDropzone(true);
          if (item.type === "__NATIVE_FILE__") {
            onDropFile(item.files[0], { valueIdx: props.value.length });

            return;
          }

          if (isMovedObject(item)) {
            return props.onChange(
              addConcept(
                addValue(props.value, newValue),
                props.value.length,
                copyConcept(item),
              ),
            );
          }

          if (props.isValidConcept && !props.isValidConcept(item)) return;

          return props.onChange(
            addConcept(
              addValue(props.value, newValue),
              props.value.length, // Assuming the last index has increased after addValue
              initializeConcept(item, defaults, tableConfig),
            ),
          );
        }}
        items={props.value.map((row, i) => (
          <>
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
                      onImportLines={(lines) =>
                        onImportLines(lines, { valueIdx: i, conceptIdx: j })
                      }
                      onDrop={(
                        item: DragItemConceptTreeNode | DragItemFile,
                      ) => {
                        if (item.type === "__NATIVE_FILE__") {
                          onDropFile(item.files[0], {
                            valueIdx: i,
                            conceptIdx: j,
                          });

                          return;
                        }

                        if (isMovedObject(item)) {
                          return props.onChange(
                            setConcept(props.value, i, j, copyConcept(item)),
                          );
                        }

                        if (props.isValidConcept && !props.isValidConcept(item))
                          return null;

                        return props.onChange(
                          setConcept(
                            props.value,
                            i,
                            j,
                            initializeConcept(item, defaults, tableConfig),
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
          </>
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
            const { valueIdx, conceptIdx } = editedFormQueryNodePosition;
            props.onChange(
              setConceptProperties(props.value, valueIdx, conceptIdx, {
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
