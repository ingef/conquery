import styled from "@emotion/styled";
import { faCalendar, faTrashCan } from "@fortawesome/free-regular-svg-icons";
import {
  faBan,
  faCircleNodes,
  faExpandArrowsAlt,
  faRefresh,
  faTrash,
} from "@fortawesome/free-solid-svg-icons";
import { createId } from "@paralleldrive/cuid2";
import { useCallback, useMemo, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import {
  DragItemConceptTreeNode,
  DragItemQuery,
} from "../standard-query-editor/types";
import Dropzone from "../ui-components/Dropzone";

import { Connector, Grid } from "./EditorLayout";
import { TreeNode } from "./TreeNode";
import { EDITOR_DROP_TYPES } from "./config";
import { useConnectorEditing } from "./connector-update/useConnectorRotation";
import { DateModal } from "./date-restriction/DateModal";
import { useDateEditing } from "./date-restriction/useDateEditing";
import { useExpandQuery } from "./expand/useExpandQuery";
import { useNegationEditing } from "./negation/useNegationEditing";
import { Tree } from "./types";
import { findNodeById, useTranslatedConnection } from "./util";

const Root = styled("div")`
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

  const onReset = () => {
    setTree(undefined);
  };

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
    setSelectedNodeId,
  };
};

export function EditorV2({
  featureDates,
  featureNegate,
  featureExpand,
  featureConnectorRotate,
}: {
  featureDates: boolean;
  featureNegate: boolean;
  featureExpand: boolean;
  featureConnectorRotate: boolean;
}) {
  const { t } = useTranslation();
  const {
    tree,
    setTree,
    updateTreeNode,
    onReset,
    selectedNode,
    setSelectedNodeId,
  } = useEditorState();

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

  useHotkeys("del", onDelete, [onDelete]);
  useHotkeys("backspace", onDelete, [onDelete]);
  useHotkeys("f", onFlip, [onFlip]);

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
    hotkey: "n",
    selectedNode,
    updateTreeNode,
  });

  const { onRotateConnector } = useConnectorEditing({
    enabled: featureConnectorRotate,
    hotkey: "c",
    selectedNode,
    updateTreeNode,
  });

  const connection = useTranslatedConnection(
    selectedNode?.children?.connection,
  );

  return (
    <Root
      onClick={() => {
        if (!selectedNode || showModal) return;
        setSelectedNodeId(undefined);
      }}
    >
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
      <Actions>
        <Flex>
          {featureDates && selectedNode && (
            <IconButton
              icon={faCalendar}
              onClick={(e) => {
                e.stopPropagation();
                onOpen();
              }}
            >
              {t("editorV2.dates")}
            </IconButton>
          )}
          {featureNegate && selectedNode && (
            <IconButton
              icon={faBan}
              onClick={(e) => {
                e.stopPropagation();
                onNegateClick();
              }}
            >
              {t("editorV2.negate")}
            </IconButton>
          )}
          {selectedNode?.children && (
            <IconButton
              icon={faRefresh}
              onClick={(e) => {
                e.stopPropagation();
                onFlip();
              }}
            >
              {t("editorV2.flip")}
            </IconButton>
          )}
          {featureConnectorRotate && selectedNode?.children && (
            <SxIconButton
              icon={faCircleNodes}
              onClick={(e) => {
                e.stopPropagation();
                onRotateConnector();
              }}
            >
              <span>{t("editorV2.connector")}</span>
              <Connector>{connection}</Connector>
            </SxIconButton>
          )}
          {canExpand && (
            <IconButton
              icon={faExpandArrowsAlt}
              onClick={(e) => {
                e.stopPropagation();
                onExpand();
              }}
            >
              {t("editorV2.expand")}
            </IconButton>
          )}
          {selectedNode && (
            <IconButton
              icon={faTrashCan}
              onClick={(e) => {
                e.stopPropagation();
                onDelete();
              }}
            >
              {t("editorV2.delete")}
            </IconButton>
          )}
        </Flex>
        <IconButton icon={faTrash} onClick={onReset}>
          {t("editorV2.clear")}
        </IconButton>
      </Actions>
      <Grid>
        {tree ? (
          <TreeNode
            tree={tree}
            updateTreeNode={updateTreeNode}
            selectedNode={selectedNode}
            setSelectedNodeId={setSelectedNodeId}
            droppable={{ h: true, v: true }}
          />
        ) : (
          <SxDropzone
            onDrop={(item) => {
              setTree({
                id: createId(),
                data: item as DragItemConceptTreeNode | DragItemQuery,
              });
            }}
            acceptedDropTypes={EDITOR_DROP_TYPES}
          >
            {() => <div>{t("editorV2.initialDropText")}</div>}
          </SxDropzone>
        )}
      </Grid>
    </Root>
  );
}
