import styled from "@emotion/styled";
import { faCalendar, faTrashCan } from "@fortawesome/free-regular-svg-icons";
import {
  faBan,
  faCircleNodes,
  faEdit,
  faExpandArrowsAlt,
  faHourglass,
  faRefresh,
  faTrash,
} from "@fortawesome/free-solid-svg-icons";
import { createId } from "@paralleldrive/cuid2";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { useDatasetId } from "../dataset/selectors";
import { nodeIsConceptQueryNode, useActiveState } from "../model/node";
import { EmptyQueryEditorDropzone } from "../standard-query-editor/EmptyQueryEditorDropzone";
import {
  DragItemConceptTreeNode,
  DragItemQuery,
} from "../standard-query-editor/types";
import { ConfirmableTooltip } from "../tooltip/ConfirmableTooltip";
import WithTooltip from "../tooltip/WithTooltip";
import Dropzone from "../ui-components/Dropzone";

import { Connector, Grid } from "./EditorLayout";
import { EditorV2QueryRunner } from "./EditorV2QueryRunner";
import { KeyboardShortcutTooltip } from "./KeyboardShortcutTooltip";
import { TreeNode } from "./TreeNode";
import { EDITOR_DROP_TYPES, HOTKEYS } from "./config";
import { useConnectorEditing } from "./connector-update/useConnectorRotation";
import { DateModal } from "./date-restriction/DateModal";
import { useDateEditing } from "./date-restriction/useDateEditing";
import { useExpandQuery } from "./expand/useExpandQuery";
import { useNegationEditing } from "./negation/useNegationEditing";
import { EditorV2QueryNodeEditor } from "./query-node-edit/EditorV2QueryNodeEditor";
import { useQueryNodeEditing } from "./query-node-edit/useQueryNodeEditing";
import { TimeConnectionModal } from "./time-connection/TimeConnectionModal";
import { useTimeConnectionEditing } from "./time-connection/useTimeConnectionEditing";
import { Tree, TreeChildrenTime } from "./types";
import { findNodeById, useGetTranslatedConnection } from "./util";

const Root = styled("div")`
  flex-grow: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
`;

const Main = styled("div")`
  flex-grow: 1;
  height: 100%;
  padding: 8px 10px 10px 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

const SxDropzone = styled(Dropzone)`
  width: 100%;
  height: 100%;
`;

const Actions = styled("div")`
  display: flex;
  align-items: center;
  justify-content: space-between;
`;

const Flex = styled("div")`
  display: flex;
  align-items: center;
`;

const SxIconButton = styled(IconButton)`
  display: flex;
  align-items: center;
  gap: 5px;
`;

const useEditorState = () => {
  const [tree, setTree] = useState<Tree | undefined>(undefined);
  const [selectedNodeId, setSelectedNodeId] = useState<Tree["id"] | undefined>(
    undefined,
  );
  const selectedNode = useMemo(() => {
    if (!tree || !selectedNodeId) {
      return undefined;
    }
    return findNodeById(tree, selectedNodeId);
  }, [tree, selectedNodeId]);

  const { active: selectedNodeActive } = useActiveState(selectedNode?.data);

  const onReset = useCallback(() => {
    setTree(undefined);
  }, []);

  const updateTreeNode = useCallback(
    (id: string, update: (node: Tree) => void) => {
      const newTree = JSON.parse(JSON.stringify(tree));
      const node = findNodeById(newTree, id);
      if (node) {
        update(node);
        setTree(newTree);
      }
    },
    [tree],
  );

  return {
    tree,
    setTree,
    updateTreeNode,
    onReset,
    selectedNode,
    selectedNodeActive,
    setSelectedNodeId,
  };
};

const useResetOnDatasetChange = (onReset: () => void) => {
  const datasetId = useDatasetId();
  useEffect(() => {
    onReset();
  }, [datasetId, onReset]);
};

export function EditorV2({
  featureDates,
  featureNegate,
  featureExpand,
  featureConnectorRotate,
  featureQueryNodeEdit,
  featureContentInfos,
  featureTimebasedQueries,
}: {
  featureDates: boolean;
  featureNegate: boolean;
  featureExpand: boolean;
  featureConnectorRotate: boolean;
  featureQueryNodeEdit: boolean;
  featureContentInfos: boolean;
  featureTimebasedQueries: boolean;
}) {
  const { t } = useTranslation();
  const {
    tree,
    setTree,
    updateTreeNode,
    onReset,
    selectedNode,
    selectedNodeActive,
    setSelectedNodeId,
  } = useEditorState();

  useResetOnDatasetChange(onReset);

  const onFlip = useCallback(() => {
    if (!selectedNode || !selectedNode.children) return;

    updateTreeNode(selectedNode.id, (node) => {
      if (!node.children) return;

      node.children.direction =
        node.children.direction === "horizontal" ? "vertical" : "horizontal";
    });
  }, [selectedNode, updateTreeNode]);

  const onDelete = useCallback(() => {
    if (!selectedNode) return;

    if (selectedNode.parentId === undefined) {
      setTree(undefined);
    } else {
      updateTreeNode(selectedNode.parentId, (parent) => {
        if (!parent.children) return;

        parent.children.items = parent.children.items.filter(
          (item) => item.id !== selectedNode.id,
        );

        if (parent.children.items.length === 1) {
          const child = parent.children.items[0];
          parent.id = child.id;
          parent.children = child.children;
          parent.data = child.data;
          parent.dates ||= child.dates;
          parent.negation ||= child.negation;
        }
      });
    }
  }, [selectedNode, setTree, updateTreeNode]);

  useHotkeys(HOTKEYS.delete.keyname, onDelete, [onDelete]);
  useHotkeys(HOTKEYS.flip.keyname, onFlip, [onFlip]);
  useHotkeys(HOTKEYS.reset.keyname, onReset, [onReset]);

  const { canExpand, onExpand } = useExpandQuery({
    enabled: featureExpand,
    hotkey: "x",
    updateTreeNode,
    selectedNode,
    setSelectedNodeId,
    tree,
  });

  const { showModal, headline, onOpen, onClose } = useDateEditing({
    enabled: featureDates,
    hotkey: "d",
    selectedNode,
  });

  const { onNegateClick } = useNegationEditing({
    enabled: featureNegate,
    hotkey: HOTKEYS.negate.keyname,
    selectedNode,
    updateTreeNode,
  });

  const { onRotateConnector } = useConnectorEditing({
    enabled: featureConnectorRotate,
    timebasedQueriesEnabled: featureTimebasedQueries,
    hotkey: HOTKEYS.rotateConnector.keyname,
    selectedNode,
    updateTreeNode,
  });

  const {
    showModal: showTimeModal,
    onOpen: onOpenTimeModal,
    onClose: onCloseTimeModal,
  } = useTimeConnectionEditing({
    enabled: featureTimebasedQueries,
    hotkey: HOTKEYS.editTimeConnection.keyname,
    selectedNode,
  });

  const {
    showModal: showQueryNodeEditor,
    onOpen: onOpenQueryNodeEditor,
    onClose: onCloseQueryNodeEditor,
  } = useQueryNodeEditing({
    enabled: featureQueryNodeEdit,
    hotkey: HOTKEYS.editQueryNode.keyname,
    selectedNode,
  });

  const getTranslatedConnection = useGetTranslatedConnection();
  const connection = getTranslatedConnection(
    selectedNode?.children?.connection,
  );

  const onChangeData = useCallback(
    (data: DragItemConceptTreeNode) => {
      if (!selectedNode) return;
      updateTreeNode(selectedNode.id, (node) => {
        node.data = data;
      });
    },
    [selectedNode, updateTreeNode],
  );

  return (
    <Root>
      <Main>
        {showQueryNodeEditor &&
          selectedNode?.data &&
          nodeIsConceptQueryNode(selectedNode.data) && (
            <EditorV2QueryNodeEditor
              onClose={onCloseQueryNodeEditor}
              node={selectedNode.data}
              onChange={onChangeData}
            />
          )}
        {showModal && selectedNode && (
          <DateModal
            onClose={onClose}
            headline={headline}
            dateRange={selectedNode.dates?.restriction}
            excludeFromDates={selectedNode.dates?.excluded}
            setExcludeFromDates={(excluded) => {
              updateTreeNode(selectedNode.id, (node) => {
                if (!node.dates) node.dates = {};
                node.dates.excluded = excluded;
              });
            }}
            onResetDates={() =>
              updateTreeNode(selectedNode.id, (node) => {
                if (!node.dates) return;
                node.dates.restriction = undefined;
              })
            }
            setDateRange={(dateRange) => {
              updateTreeNode(selectedNode.id, (node) => {
                if (!node.dates) node.dates = {};
                node.dates.restriction = dateRange;
              });
            }}
          />
        )}
        {showTimeModal && selectedNode && (
          <TimeConnectionModal
            onClose={onCloseTimeModal}
            conditions={selectedNode.children as TreeChildrenTime}
            onChange={(nodeChildren) => {
              updateTreeNode(selectedNode.id, (node) => {
                node.children = nodeChildren;
              });
            }}
          />
        )}
        {tree && (
          <Actions>
            <Flex>
              {featureQueryNodeEdit &&
                selectedNode?.data &&
                nodeIsConceptQueryNode(selectedNode.data) && (
                  <KeyboardShortcutTooltip
                    keyname={HOTKEYS.editQueryNode.keyname}
                  >
                    <IconButton
                      icon={faEdit}
                      tight
                      active={selectedNodeActive}
                      onClick={(e) => {
                        e.stopPropagation();
                        onOpenQueryNodeEditor();
                      }}
                    >
                      {t("editorV2.edit")}
                    </IconButton>
                  </KeyboardShortcutTooltip>
                )}
              {featureDates && selectedNode && (
                <KeyboardShortcutTooltip keyname={HOTKEYS.editDates.keyname}>
                  <IconButton
                    icon={faCalendar}
                    tight
                    active={!!selectedNode.dates?.restriction}
                    onClick={(e) => {
                      e.stopPropagation();
                      onOpen();
                    }}
                  >
                    {t("editorV2.dates")}
                  </IconButton>
                </KeyboardShortcutTooltip>
              )}
              {featureNegate && selectedNode && (
                <KeyboardShortcutTooltip keyname={HOTKEYS.negate.keyname}>
                  <IconButton
                    icon={faBan}
                    tight
                    active={selectedNode.negation}
                    red={selectedNode.negation}
                    onClick={(e) => {
                      e.stopPropagation();
                      onNegateClick();
                    }}
                  >
                    {t("editorV2.negate")}
                  </IconButton>
                </KeyboardShortcutTooltip>
              )}
              {featureConnectorRotate && selectedNode?.children && (
                <KeyboardShortcutTooltip
                  keyname={HOTKEYS.rotateConnector.keyname}
                >
                  <SxIconButton
                    icon={faCircleNodes}
                    tight
                    onClick={(e) => {
                      e.stopPropagation();
                      onRotateConnector();
                    }}
                  >
                    <Connector>{connection}</Connector>
                  </SxIconButton>
                </KeyboardShortcutTooltip>
              )}
              {selectedNode?.children?.connection === "time" && (
                <KeyboardShortcutTooltip
                  keyname={HOTKEYS.editTimeConnection.keyname}
                >
                  <SxIconButton
                    icon={faHourglass}
                    tight
                    onClick={(e) => {
                      e.stopPropagation();
                      onOpenTimeModal();
                    }}
                  >
                    <span>{t("editorV2.timeConnection")}</span>
                  </SxIconButton>
                </KeyboardShortcutTooltip>
              )}
              {canExpand && (
                <KeyboardShortcutTooltip keyname={HOTKEYS.expand.keyname}>
                  <IconButton
                    icon={faExpandArrowsAlt}
                    tight
                    onClick={(e) => {
                      e.stopPropagation();
                      onExpand();
                    }}
                  >
                    {t("editorV2.expand")}
                  </IconButton>
                </KeyboardShortcutTooltip>
              )}
            </Flex>
            <Flex>
              {selectedNode?.children && (
                <KeyboardShortcutTooltip keyname={HOTKEYS.flip.keyname}>
                  <IconButton
                    icon={faRefresh}
                    tight
                    onClick={(e) => {
                      e.stopPropagation();
                      onFlip();
                    }}
                  >
                    {t("editorV2.flip")}
                  </IconButton>
                </KeyboardShortcutTooltip>
              )}
              {selectedNode && (
                <KeyboardShortcutTooltip
                  keyname={HOTKEYS.delete.keyname.join(" | ")}
                >
                  <IconButton
                    icon={faTrashCan}
                    tight
                    onClick={(e) => {
                      e.stopPropagation();
                      onDelete();
                    }}
                  >
                    {t("editorV2.delete")}
                  </IconButton>
                </KeyboardShortcutTooltip>
              )}
              <ConfirmableTooltip
                onConfirm={onReset}
                confirmationText={t("editorV2.clearConfirm")}
              >
                <WithTooltip text={t("editorV2.clear")}>
                  <IconButton
                    style={{ marginLeft: "20px", height: "32.5px" }}
                    icon={faTrash}
                  />
                </WithTooltip>
              </ConfirmableTooltip>
            </Flex>
          </Actions>
        )}
        <Grid
          onClick={() => {
            if (!selectedNode || showModal) return;
            setSelectedNodeId(undefined);
          }}
        >
          {tree ? (
            <TreeNode
              tree={tree}
              updateTreeNode={updateTreeNode}
              selectedNode={selectedNode}
              setSelectedNodeId={setSelectedNodeId}
              droppable={{ h: true, v: true }}
              featureContentInfos={featureContentInfos}
              onOpenQueryNodeEditor={onOpenQueryNodeEditor}
              onOpenTimeModal={onOpenTimeModal}
              onRotateConnector={onRotateConnector}
            />
          ) : (
            <SxDropzone
              onDrop={(item) => {
                const id = createId();
                setTree({
                  id,
                  data: item as DragItemConceptTreeNode | DragItemQuery,
                });
                setSelectedNodeId(id);
              }}
              acceptedDropTypes={EDITOR_DROP_TYPES}
            >
              {() => <EmptyQueryEditorDropzone />}
            </SxDropzone>
          )}
        </Grid>
      </Main>
      <EditorV2QueryRunner query={{ tree }} />
    </Root>
  );
}
