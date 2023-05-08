import styled from "@emotion/styled";
import { createId } from "@paralleldrive/cuid2";
import { useTranslation } from "react-i18next";

import { DNDType } from "../common/constants/dndTypes";
import { nodeIsConceptQueryNode } from "../model/node";
import { getRootNodeLabel } from "../standard-query-editor/helper";
import Dropzone, { DropzoneProps } from "../ui-components/Dropzone";

import { Connector, Grid } from "./EditorLayout";
import { EDITOR_DROP_TYPES } from "./config";
import { DateRange } from "./date-restriction/DateRange";
import { Tree } from "./types";

const NodeContainer = styled("div")`
  display: grid;
  gap: 5px;
`;

const Node = styled("div")<{
  selected?: boolean;
  negated?: boolean;
  leaf?: boolean;
}>`
  padding: ${({ leaf }) => (leaf ? "8px 10px" : "12px")};
  border: 1px solid
    ${({ negated, theme, selected }) =>
      negated ? "red" : selected ? theme.col.gray : theme.col.grayMediumLight};
  box-shadow: ${({ selected, theme }) =>
    selected ? `inset 0px 0px 0px 1px ${theme.col.gray}` : "none"};

  border-radius: ${({ theme }) => theme.borderRadius};
  width: ${({ leaf }) => (leaf ? "150px" : "inherit")};
  background-color: ${({ leaf, theme }) => (leaf ? "white" : theme.col.bg)};
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

function getGridStyles(tree: Tree) {
  if (!tree.children) {
    return {};
  }

  if (tree.children.direction === "horizontal") {
    return {
      gridAutoFlow: "column",
    };
  } else {
    return {
      gridTemplateColumns: "1fr",
    };
  }
}

const InvisibleDropzoneContainer = styled(Dropzone)`
  width: 100%;
  height: 100%;
`;

const InvisibleDropzone = (
  props: Omit<DropzoneProps<any>, "acceptedDropTypes">,
) => {
  return (
    <InvisibleDropzoneContainer
      invisible
      naked
      acceptedDropTypes={EDITOR_DROP_TYPES}
      {...props}
    />
  );
};

const Name = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
  font-weight: 400;
`;

const Description = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
`;

const PreviousQueryLabel = styled("p")`
  margin: 0 0 4px;
  line-height: 1.2;
  font-size: ${({ theme }) => theme.font.xs};
  text-transform: uppercase;
  font-weight: 700;
  color: ${({ theme }) => theme.col.blueGrayDark};
`;

const RootNode = styled("p")`
  margin: 0 0 4px;
  line-height: 1;
  text-transform: uppercase;
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.blueGrayDark};
  word-break: break-word;
`;

const Dates = styled("div")`
  text-align: right;
`;

export function TreeNode({
  tree,
  updateTreeNode,
  droppable,
  selectedNode,
  setSelectedNodeId,
}: {
  tree: Tree;
  updateTreeNode: (id: string, update: (node: Tree) => void) => void;
  droppable: {
    h: boolean;
    v: boolean;
  };
  selectedNode: Tree | undefined;
  setSelectedNodeId: (id: Tree["id"] | undefined) => void;
}) {
  const gridStyles = getGridStyles(tree);

  const { t } = useTranslation();

  const rootNodeLabel = tree.data ? getRootNodeLabel(tree.data) : null;

  const onDropOutsideOfNode = ({
    pos,
    direction,
    item,
  }: {
    direction: "h" | "v";
    pos: "b" | "a";
    item: any;
  }) => {
    // Create a new "parent" and create a new "item", make parent contain tree and item
    const newParentId = createId();
    const newItemId = createId();

    updateTreeNode(tree.id, (node) => {
      const newChildren: Tree[] = [
        {
          id: newItemId,
          negation: false,
          data: item,
          parentId: newParentId,
        },
        {
          ...tree,
          parentId: newParentId,
        },
      ];

      node.id = newParentId;
      node.data = undefined;
      node.dates = undefined;
      node.children = {
        connection: tree.children?.connection === "and" ? "or" : "and" || "and",
        direction: direction === "h" ? "horizontal" : "vertical",
        items: pos === "b" ? newChildren : newChildren.reverse(),
      };
    });
    setSelectedNodeId(newItemId);
  };

  const onDropAtChildrenIdx = ({ idx, item }: { idx: number; item: any }) => {
    const newItemId = createId();
    // Create a new "item" and insert it at idx of tree.children
    updateTreeNode(tree.id, (node) => {
      if (node.children) {
        node.children.items.splice(idx, 0, {
          id: newItemId,
          negation: false,
          data: item,
          parentId: node.id,
        });
      }
    });
    setSelectedNodeId(newItemId);
  };

  return (
    <NodeContainer>
      {droppable.v && (
        <InvisibleDropzone
          onDrop={(item) =>
            onDropOutsideOfNode({ pos: "b", direction: "v", item })
          }
        />
      )}
      <NodeContainer
        style={{
          gridAutoFlow: "column",
        }}
      >
        {droppable.h && (
          <InvisibleDropzone
            onDrop={(item) =>
              onDropOutsideOfNode({
                pos: "b",
                direction: "h",
                item,
              })
            }
          />
        )}
        <Node
          negated={tree.negation}
          leaf={!tree.children}
          selected={selectedNode?.id === tree.id}
          onClick={(e) => {
            e.stopPropagation();
            setSelectedNodeId(tree.id);
          }}
        >
          {tree.dates?.restriction && (
            <Dates>
              <DateRange dateRange={tree.dates.restriction} />
            </Dates>
          )}
          {(!tree.children || tree.data) && (
            <div>
              {tree.data?.type !== DNDType.CONCEPT_TREE_NODE && (
                <PreviousQueryLabel>
                  {t("queryEditor.previousQuery")}
                </PreviousQueryLabel>
              )}
              {rootNodeLabel && <RootNode>{rootNodeLabel}</RootNode>}
              {tree.data?.label && <Name>{tree.data.label}</Name>}
              {tree.data && nodeIsConceptQueryNode(tree.data) && (
                <Description>{tree.data?.description}</Description>
              )}
            </div>
          )}
          {tree.children && (
            <Grid style={gridStyles}>
              <InvisibleDropzone
                key="dropzone-before"
                onDrop={(item) => onDropAtChildrenIdx({ idx: 0, item })}
              />
              {tree.children.items.map((item, i, items) => (
                <>
                  <TreeNode
                    key={item.id}
                    tree={item}
                    updateTreeNode={updateTreeNode}
                    selectedNode={selectedNode}
                    setSelectedNodeId={setSelectedNodeId}
                    droppable={{
                      h:
                        !item.children &&
                        tree.children?.direction === "vertical",
                      v:
                        !item.children &&
                        tree.children?.direction === "horizontal",
                    }}
                  />
                  {i < items.length - 1 && (
                    <InvisibleDropzoneContainer
                      key={item.id + "connector"}
                      acceptedDropTypes={[DNDType.CONCEPT_TREE_NODE]}
                      naked
                      bare
                      transparent
                      onDrop={(item) =>
                        onDropAtChildrenIdx({ idx: i + 1, item })
                      }
                    >
                      {() => <Connector>{tree.children?.connection}</Connector>}
                    </InvisibleDropzoneContainer>
                  )}
                </>
              ))}
              <InvisibleDropzone
                key="dropzone-after"
                onDrop={(item) =>
                  onDropAtChildrenIdx({
                    idx: tree.children!.items.length,
                    item,
                  })
                }
              />
            </Grid>
          )}
        </Node>
        {droppable.h && (
          <InvisibleDropzone
            onDrop={(item) =>
              onDropOutsideOfNode({ pos: "a", direction: "h", item })
            }
          />
        )}
      </NodeContainer>
      {droppable.v && (
        <InvisibleDropzone
          onDrop={(item) =>
            onDropOutsideOfNode({ pos: "a", direction: "v", item })
          }
        />
      )}
    </NodeContainer>
  );
}
