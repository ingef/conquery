import styled from "@emotion/styled";
import { faCalendar, faTrashCan } from "@fortawesome/free-regular-svg-icons";
import {
  faBan,
  faCircleNodes,
  faRefresh,
  faTrash,
} from "@fortawesome/free-solid-svg-icons";
import { createId } from "@paralleldrive/cuid2";
import { useCallback, useMemo, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";

import { useGetQuery } from "../api/api";
import {
  AndNodeT,
  DateRangeT,
  DateRestrictionNodeT,
  NegationNodeT,
  OrNodeT,
  QueryConceptNodeT,
  SavedQueryNodeT,
} from "../api/types";
import IconButton from "../button/IconButton";
import { DNDType } from "../common/constants/dndTypes";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import Dropzone, { DropzoneProps } from "../ui-components/Dropzone";

import { DateModal, useDateEditing } from "./DateModal";
import { Tree } from "./types";

const Root = styled("div")`
  flex-grow: 1;
  height: 100%;
  padding: 8px 10px 10px 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

const Grid = styled("div")`
  flex-grow: 1;
  display: grid;
  gap: 3px;
  height: 100%;
  width: 100%;
  place-items: center;
  overflow: auto;
`;

const SxDropzone = styled(Dropzone)`
  width: 200px;
  height: 100px;
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

const findNodeById = (tree: Tree, id: string): Tree | undefined => {
  if (tree.id === id) {
    return tree;
  }
  if (tree.children) {
    for (const child of tree.children.items) {
      const found = findNodeById(child, id);
      if (found) {
        return found;
      }
    }
  }
  return undefined;
};

const useEditorState = () => {
  const [tree, setTree] = useState<Tree | undefined>(undefined);
  const [selectedNodeId, setSelectedNodeId] = useState<Tree | undefined>(
    undefined,
  );
  const selectedNode = useMemo(() => {
    if (!tree || !selectedNodeId) {
      return undefined;
    }
    return findNodeById(tree, selectedNodeId.id);
  }, [tree, selectedNodeId]);

  const expandNode = (
    queryNode:
      | AndNodeT
      | DateRestrictionNodeT
      | OrNodeT
      | NegationNodeT
      | QueryConceptNodeT
      | SavedQueryNodeT,
    config: {
      parentId?: string;
      negation?: boolean;
      dateRestriction?: DateRangeT;
    } = {},
  ): Tree => {
    switch (queryNode.type) {
      case "AND":
        if (queryNode.children.length === 1) {
          return expandNode(queryNode.children[0], config);
        }
        const andid = createId();
        return {
          id: andid,
          ...config,
          children: {
            connection: "and",
            direction: "horizontal",
            items: queryNode.children.map((child) =>
              expandNode(child, { parentId: andid }),
            ),
          },
        };
      case "OR":
        if (queryNode.children.length === 1) {
          return expandNode(queryNode.children[0], config);
        }
        const orid = createId();
        return {
          id: orid,
          ...config,
          children: {
            connection: "or",
            direction: "vertical",
            items: queryNode.children.map((child) =>
              expandNode(child, { parentId: orid }),
            ),
          },
        };
      case "NEGATION":
        return expandNode(queryNode.child, { ...config, negation: true });
      case "DATE_RESTRICTION":
        return expandNode(queryNode.child, {
          ...config,
          dateRestriction: queryNode.dateRange,
        });
      case "CONCEPT":
        const concept = getConceptById(queryNode.ids[0]);
        return {
          id: createId(),
          data: concept,
          ...config,
        };
      case "SAVED_QUERY":
        return {
          id: queryNode.query,
          data: queryNode,
          ...config,
        };
    }
  };

  const getQuery = useGetQuery();
  const expandQuery = async (id: string) => {
    const query = await getQuery(id);

    if (query && query.query.root.type !== "EXTERNAL_RESOLVED") {
      const tree = expandNode(query.query.root);
      setTree(tree);
    }
  };

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
    expandQuery,
    tree,
    setTree,
    updateTreeNode,
    onReset,
    selectedNode,
    setSelectedNodeId,
  };
};

const DROP_TYPES = [
  DNDType.PREVIOUS_QUERY,
  DNDType.PREVIOUS_SECONDARY_ID_QUERY,
];

const useNegationEditing = ({
  selectedNode,
  updateTreeNode,
  enabled,
}: {
  enabled: boolean;
  selectedNode: Tree | undefined;
  updateTreeNode: (id: string, update: (node: Tree) => void) => void;
}) => {
  const onNegateClick = useCallback(() => {
    if (!selectedNode || !enabled) return;

    updateTreeNode(selectedNode.id, (node) => {
      node.negation = !node.negation;
    });
  }, [enabled, selectedNode, updateTreeNode]);

  useHotkeys("n", onNegateClick, [onNegateClick]);

  return {
    onNegateClick,
  };
};

const CONNECTORS = ["and", "or", "before"] as const;
const getNextConnector = (connector: (typeof CONNECTORS)[number]) => {
  const index = CONNECTORS.indexOf(connector);
  return CONNECTORS[(index + 1) % CONNECTORS.length];
};

export function EditorV2({
  featureDates,
  featureNegate,
}: {
  featureDates: boolean;
  featureNegate: boolean;
}) {
  const {
    tree,
    setTree,
    updateTreeNode,
    expandQuery,
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
          parent.dateRestriction ||= child.dateRestriction;
          parent.negation ||= child.negation;
        }
      });
    }
  }, [selectedNode, setTree, updateTreeNode]);

  const onRotateConnector = useCallback(() => {
    if (!selectedNode || !selectedNode.children) return;

    updateTreeNode(selectedNode.id, (node) => {
      if (!node.children) return;

      node.children.connection = getNextConnector(node.children.connection);
    });
  }, [selectedNode, updateTreeNode]);

  useHotkeys("del", onDelete, [onDelete]);
  useHotkeys("backspace", onDelete, [onDelete]);
  useHotkeys("f", onFlip, [onFlip]);
  useHotkeys("c", onRotateConnector, [onRotateConnector]);

  const { showModal, headline, onOpen, onClose } = useDateEditing({
    enabled: featureDates,
    selectedNode,
  });

  const { onNegateClick } = useNegationEditing({
    enabled: featureNegate,
    selectedNode,
    updateTreeNode,
  });

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
          dateRange={selectedNode.dateRestriction}
          onResetDates={() =>
            updateTreeNode(selectedNode.id, (node) => {
              node.dateRestriction = undefined;
            })
          }
          setDateRange={(dateRange) => {
            updateTreeNode(selectedNode.id, (node) => {
              node.dateRestriction = dateRange;
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
              Dates
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
              Negate
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
              Flip
            </IconButton>
          )}
          {selectedNode?.children && (
            <SxIconButton
              icon={faCircleNodes}
              onClick={(e) => {
                e.stopPropagation();
                onRotateConnector();
              }}
            >
              <span>Connected:</span>
              <Connector>{selectedNode.children.connection}</Connector>
            </SxIconButton>
          )}
          {selectedNode && (
            <IconButton
              icon={faTrashCan}
              onClick={(e) => {
                e.stopPropagation();
                onDelete();
              }}
            >
              Delete
            </IconButton>
          )}
        </Flex>
        <IconButton icon={faTrash} onClick={onReset}>
          Clear
        </IconButton>
      </Actions>
      <Grid>
        {tree ? (
          <TreeNode
            tree={tree}
            updateTreeNode={updateTreeNode}
            selectedNode={selectedNode}
            setSelectedNode={setSelectedNodeId}
            droppable={{ h: true, v: true }}
          />
        ) : (
          <SxDropzone
            onDrop={(droppedItem) => {
              expandQuery((droppedItem as any).id);
            }}
            acceptedDropTypes={DROP_TYPES}
          >
            {() => <div>Drop if you dare</div>}
          </SxDropzone>
        )}
      </Grid>
    </Root>
  );
}

const NodeContainer = styled("div")`
  display: grid;
  gap: 5px;
`;

const Node = styled("div")<{
  selected?: boolean;
  negated?: boolean;
  leaf?: boolean;
}>`
  padding: ${({ leaf }) => (leaf ? "5px 10px" : "10px")};
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

const Connector = styled("span")`
  text-transform: uppercase;
  font-size: ${({ theme }) => theme.font.sm};
  color: black;

  border-radius: ${({ theme }) => theme.borderRadius};
  padding: 0px 5px;
  display: flex;
  justify-content: center;
  align-items: center;
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
      acceptedDropTypes={[DNDType.CONCEPT_TREE_NODE]}
      {...props}
    />
  );
};

const Name = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
  font-weight: 700;
`;

const Description = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
`;

const DateRange = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  font-family: monospace;
`;

const formatDateRange = (range: DateRangeT): string => {
  if (range.min === range.max) {
    return range.min || "";
  } else {
    return `${range.min} - ${range.max}`;
  }
};

function TreeNode({
  tree,
  updateTreeNode,
  droppable,
  selectedNode,
  setSelectedNode,
}: {
  tree: Tree;
  updateTreeNode: (id: string, update: (node: Tree) => void) => void;
  droppable: {
    h: boolean;
    v: boolean;
  };
  selectedNode: Tree | undefined;
  setSelectedNode: (node: Tree | undefined) => void;
}) {
  const gridStyles = getGridStyles(tree);

  const onDropOutsideOfNode = ({
    pos,
    direction,
    item,
  }: {
    direction: "h" | "v";
    pos: "b" | "a";
    item: any;
  }) => {
    console.log("dropped outside of node", { pos, direction, item });
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
          id: tree.id,
          negation: false,
          data: tree.data,
          children: tree.children,
          parentId: newParentId,
        },
      ];

      node.id = newParentId;
      node.data = undefined;
      node.children = {
        connection: tree.children?.connection === "and" ? "or" : "and" || "and",
        direction: direction === "h" ? "horizontal" : "vertical",
        items: pos === "b" ? newChildren : newChildren.reverse(),
      };
    });
  };

  const onDropAtChildrenIdx = ({ idx, item }: { idx: number; item: any }) => {
    // Create a new "item" and insert it at idx of tree.children
    updateTreeNode(tree.id, (node) => {
      if (node.children) {
        node.children.items.splice(idx, 0, {
          id: createId(),
          negation: false,
          data: item,
          parentId: node.id,
        });
      }
    });
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
            setSelectedNode(tree);
          }}
        >
          {(!tree.children || tree.data || tree.dateRestriction) && (
            <div>
              {tree.dateRestriction && (
                <>
                  <DateRange>{formatDateRange(tree.dateRestriction)}</DateRange>
                </>
              )}
              {tree.data?.label && <Name>{tree.data.label[0]}</Name>}
              {tree.data?.description && (
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
                    setSelectedNode={setSelectedNode}
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
