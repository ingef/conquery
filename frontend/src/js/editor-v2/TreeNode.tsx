import styled from "@emotion/styled";
import { faCalendarMinus } from "@fortawesome/free-regular-svg-icons";
import { createId } from "@paralleldrive/cuid2";
import { DOMAttributes, memo } from "react";
import { useTranslation } from "react-i18next";

import { DNDType } from "../common/constants/dndTypes";
import { Icon } from "../icon/FaIcon";
import { nodeIsConceptQueryNode, useActiveState } from "../model/node";
import { getRootNodeLabel } from "../standard-query-editor/helper";
import WithTooltip from "../tooltip/WithTooltip";
import Dropzone, { DropzoneProps } from "../ui-components/Dropzone";

import { Connector, Grid } from "./EditorLayout";
import { TreeNodeConcept } from "./TreeNodeConcept";
import { EDITOR_DROP_TYPES } from "./config";
import { DateRange } from "./date-restriction/DateRange";
import { TimeConnection } from "./time-connection/TimeConnection";
import { ConnectionKind, Tree } from "./types";
import { useGetTranslatedConnection } from "./util";

const NodeContainer = styled("div")`
  display: grid;
  gap: 5px;
`;

const Node = styled("div")<{
  selected?: boolean;
  active?: boolean;
  negated?: boolean;
  leaf?: boolean;
  isDragging?: boolean;
}>`
  padding: ${({ leaf, isDragging }) =>
    leaf ? "8px 10px" : isDragging ? "5px" : "24px"};
  border: 2px solid
    ${({ negated, theme, selected, active }) =>
      negated
        ? theme.col.red
        : active
        ? theme.col.blueGrayDark
        : selected
        ? theme.col.gray
        : theme.col.grayMediumLight};
  box-shadow: ${({ selected, theme }) =>
    selected ? `inset 0px 0px 0px 4px ${theme.col.blueGrayVeryLight}` : "none"};

  border-radius: ${({ theme }) => theme.borderRadius};
  width: ${({ leaf }) => (leaf ? "230px" : "inherit")};
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

const InvisibleDropzoneContainer = styled(Dropzone)<{ bare?: boolean }>`
  width: 100%;
  height: 100%;
  padding: ${({ bare }) => (bare ? "6px" : "20px")};
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
  color: ${({ theme }) => theme.col.black};
`;

const PreviousQueryLabel = styled("p")`
  margin: 0;
  line-height: 1.2;
  font-size: ${({ theme }) => theme.font.xs};
  text-transform: uppercase;
  font-weight: 700;
  color: ${({ theme }) => theme.col.blueGrayDark};
`;

const ContentContainer = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

const RootNode = styled("p")`
  margin: 0;
  line-height: 1;
  text-transform: uppercase;
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.blueGrayDark};
  word-break: break-word;
`;

const Dates = styled("div")`
  text-align: right;
  font-size: ${({ theme }) => theme.font.xs};
  text-transform: uppercase;
  font-weight: 400;
`;

export function TreeNode({
  tree,
  treeParent,
  updateTreeNode,
  droppable,
  selectedNode,
  setSelectedNodeId,
  onDoubleClick,
  featureContentInfos,
}: {
  tree: Tree;
  treeParent?: Tree;
  updateTreeNode: (id: string, update: (node: Tree) => void) => void;
  droppable: {
    h: boolean;
    v: boolean;
  };
  selectedNode: Tree | undefined;
  setSelectedNodeId: (id: Tree["id"] | undefined) => void;
  onDoubleClick?: DOMAttributes<HTMLElement>["onDoubleClick"];
  featureContentInfos?: boolean;
}) {
  const gridStyles = getGridStyles(tree);

  const { t } = useTranslation();

  const rootNodeLabel = tree.data ? getRootNodeLabel(tree.data) : null;

  const { active, tooltipText } = useActiveState(tree.data);

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
      node.negation = false;

      const connection =
        treeParent?.children?.connection || tree.children?.connection;

      node.children = {
        connection: connection === "and" ? "or" : "and" || "and",
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
        <Dropzone
          naked
          bare
          acceptedDropTypes={EDITOR_DROP_TYPES}
          onDrop={() => {}}
        >
          {({ canDrop }) => (
            <WithTooltip text={tooltipText}>
              <Node
                isDragging={canDrop}
                active={active}
                negated={tree.negation}
                leaf={!tree.children}
                selected={selectedNode?.id === tree.id}
                onDoubleClick={onDoubleClick}
                onClick={(e) => {
                  e.stopPropagation();
                  setSelectedNodeId(tree.id);
                }}
              >
                {tree.children && tree.children.connection === "time" && (
                  <TimeConnection conditions={tree.children} />
                )}
                {tree.dates?.restriction && (
                  <Dates>
                    <DateRange dateRange={tree.dates.restriction} />
                  </Dates>
                )}
                {tree.dates?.excluded && (
                  <Dates>
                    <Icon red icon={faCalendarMinus} left />
                    {t("editorV2.datesExcluded")}
                  </Dates>
                )}
                {(!tree.children || tree.data) && (
                  <ContentContainer>
                    {tree.data?.type !== DNDType.CONCEPT_TREE_NODE && (
                      <PreviousQueryLabel>
                        {t("queryEditor.previousQuery")}
                      </PreviousQueryLabel>
                    )}
                    {rootNodeLabel && <RootNode>{rootNodeLabel}</RootNode>}
                    {tree.data?.label && <Name>{tree.data.label}</Name>}
                    {tree.data && nodeIsConceptQueryNode(tree.data) && (
                      <TreeNodeConcept
                        node={tree.data}
                        featureContentInfos={featureContentInfos}
                      />
                    )}
                  </ContentContainer>
                )}
                {tree.children && (
                  <Grid style={gridStyles}>
                    <InvisibleDropzone
                      key="dropzone-before"
                      naked
                      bare
                      onDrop={(item) => onDropAtChildrenIdx({ idx: 0, item })}
                    >
                      {() => (
                        <Connection connection={tree.children?.connection} />
                      )}
                    </InvisibleDropzone>
                    {tree.children.items.map((item, i, items) => (
                      <>
                        <TreeNode
                          key={item.id}
                          featureContentInfos={featureContentInfos}
                          tree={item}
                          treeParent={tree}
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
                            {() => (
                              <Connection
                                connection={tree.children?.connection}
                              />
                            )}
                          </InvisibleDropzoneContainer>
                        )}
                      </>
                    ))}
                    <InvisibleDropzone
                      key="dropzone-after"
                      naked
                      bare
                      onDrop={(item) =>
                        onDropAtChildrenIdx({
                          idx: tree.children!.items.length,
                          item,
                        })
                      }
                    >
                      {() => (
                        <Connection connection={tree.children?.connection} />
                      )}
                    </InvisibleDropzone>
                  </Grid>
                )}
              </Node>
            </WithTooltip>
          )}
        </Dropzone>
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

const Connection = memo(({ connection }: { connection?: ConnectionKind }) => {
  const getTranslatedConnection = useGetTranslatedConnection();

  return <Connector>{getTranslatedConnection(connection)}</Connector>;
});
