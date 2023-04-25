import styled from "@emotion/styled";
import {
  faLeftRight,
  faTrash,
  faUpDown,
} from "@fortawesome/free-solid-svg-icons";
import { createId } from "@paralleldrive/cuid2";
import { useState } from "react";

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
import {
  getConceptById,
  setTree,
} from "../concept-trees/globalTreeStoreHelper";
import Dropzone, { DropzoneProps } from "../ui-components/Dropzone";

const Root = styled("div")`
  flex-grow: 1;
  height: 100%;
  padding: 8px 10px 10px 10px;
  overflow: auto;
  display: flex;
  flex-direction: column;
`;

const Grid = styled("div")`
  flex-grow: 1;
  display: grid;
  grid-gap: 3px;
  height: 100%;
  width: 100%;
  place-items: center;
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

interface Tree {
  id: string;
  parentId?: string;
  name: string;
  negation?: boolean;
  dateRestriction?: DateRangeT;
  data?: any;
  children?: {
    connection: "and" | "or" | "time";
    direction: "horizontal" | "vertical";
    items: Tree[];
  };
}

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
  const [selectedNode, setSelectedNode] = useState<Tree | undefined>(undefined);

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
        const andid = createId();
        return {
          id: andid,
          name: "AND",
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
        const orid = createId();
        return {
          id: orid,
          name: "OR",
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
          name: concept?.label || "Unknown",
          ...config,
        };
      case "SAVED_QUERY":
        return {
          id: queryNode.query,
          name: queryNode.query,
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

  return {
    expandQuery,
    tree,
    setTree,
    onReset,
    selectedNode,
    setSelectedNode,
  };
};

const DROP_TYPES = [
  DNDType.PREVIOUS_QUERY,
  DNDType.PREVIOUS_SECONDARY_ID_QUERY,
];

export function EditorV2() {
  const { tree, setTree, expandQuery, onReset, selectedNode, setSelectedNode } =
    useEditorState();

  return (
    <Root>
      <Actions>
        <Flex>
          {selectedNode && (
            <>
              <IconButton
                icon={faTrash}
                onClick={() => {
                  setTree((tr) => {
                    if (selectedNode.parentId === undefined) {
                      return undefined;
                    } else {
                      const newTree = JSON.parse(JSON.stringify(tr));
                      const parent = findNodeById(
                        newTree,
                        selectedNode.parentId,
                      );
                      if (parent?.children) {
                        parent.children.items = parent.children.items.filter(
                          (item) => item.id !== selectedNode.id,
                        );
                      }
                      return newTree;
                    }
                  });
                }}
              />
            </>
          )}
          {selectedNode?.children && (
            <>
              <IconButton
                icon={faUpDown}
                active={selectedNode?.children?.direction === "vertical"}
                onClick={() => {
                  setTree((tr) => {
                    const newTree = JSON.parse(JSON.stringify(tr));
                    const node = findNodeById(newTree, selectedNode.id);
                    if (node?.children) {
                      node.children.direction = "vertical";
                    }

                    return newTree;
                  });
                }}
              />
              <IconButton
                icon={faLeftRight}
                active={selectedNode?.children?.direction === "horizontal"}
                onClick={() => {
                  setTree((tr) => {
                    const newTree = JSON.parse(JSON.stringify(tr));
                    const node = findNodeById(newTree, selectedNode.id);
                    if (node?.children) {
                      node.children.direction = "horizontal";
                    }

                    return newTree;
                  });
                }}
              />
            </>
          )}
        </Flex>
        <IconButton icon={faTrash} onClick={onReset} />
      </Actions>
      <Grid>
        {tree ? (
          <TreeNode
            tree={tree}
            selectedNode={selectedNode}
            setSelectedNode={setSelectedNode}
            droppable={{
              h: true,
              v: true,
            }}
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
  grid-gap: 5px;
`;

const Node = styled("div")<{
  selected?: boolean;
  negated?: boolean;
  leaf?: boolean;
}>`
  padding: ${({ leaf }) => (leaf ? "5px 10px" : "10px")};
  border: 1px solid
    ${({ negated, theme, selected }) =>
      negated ? "red" : selected ? "black" : theme.col.grayMediumLight};
  border-radius: 5px;
  width: ${({ leaf }) => (leaf ? "150px" : "inherit")};
  background-color: ${({ selected, theme }) =>
    selected ? "white" : theme.col.bgAlt};
  cursor: pointer;
`;

const Connector = styled("span")`
  font-weight: 700;
  text-transform: uppercase;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
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
  padding: 5px;
`;

const InvisibleDropzone = (
  props: Omit<DropzoneProps<any>, "acceptedDropTypes">,
) => {
  return (
    <InvisibleDropzoneContainer
      invisible
      transparent
      naked
      acceptedDropTypes={[DNDType.CONCEPT_TREE_NODE]}
      {...props}
    />
  );
};

function TreeNode({
  tree,
  droppable,
  selectedNode,
  setSelectedNode,
}: {
  tree: Tree;
  droppable: {
    h: boolean;
    v: boolean;
  };
  selectedNode: Tree | undefined;
  setSelectedNode: (node: Tree | undefined) => void;
}) {
  const gridStyles = getGridStyles(tree);

  return (
    <NodeContainer>
      {droppable.v && (
        <InvisibleDropzone
          onDrop={(item) => {
            console.log(item);
          }}
        />
      )}

      <NodeContainer
        style={{
          gridAutoFlow: "column",
        }}
      >
        {droppable.h && (
          <InvisibleDropzone
            onDrop={(item) => {
              console.log(item);
            }}
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
          <span>
            {!tree.children && tree.name}
            {tree.dateRestriction ? JSON.stringify(tree.dateRestriction) : ""}
          </span>
          {tree.children && (
            <Grid style={gridStyles}>
              <InvisibleDropzone
                onDrop={(item) => {
                  console.log(item);
                }}
              />
              {tree.children.items.map((item, i, items) => (
                <>
                  <TreeNode
                    tree={item}
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
                      acceptedDropTypes={[DNDType.CONCEPT_TREE_NODE]}
                      naked
                      transparent
                      onDrop={(item) => {
                        console.log(item);
                      }}
                    >
                      {() => <Connector>{tree.children?.connection}</Connector>}
                    </InvisibleDropzoneContainer>
                  )}
                </>
              ))}
              <InvisibleDropzone
                onDrop={(item) => {
                  console.log(item);
                }}
              />
            </Grid>
          )}
        </Node>
        {droppable.h && (
          <InvisibleDropzone
            onDrop={(item) => {
              console.log(item);
            }}
          />
        )}
      </NodeContainer>
      {droppable.v && (
        <InvisibleDropzone
          onDrop={(item) => {
            console.log(item);
          }}
        />
      )}
    </NodeContainer>
  );
}
