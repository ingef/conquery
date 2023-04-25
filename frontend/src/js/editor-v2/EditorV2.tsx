import styled from "@emotion/styled";
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
import { DNDType } from "../common/constants/dndTypes";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import Dropzone, { DropzoneProps } from "../ui-components/Dropzone";

const Root = styled("div")`
  flex-grow: 1;
  height: 100%;
  padding: 8px 10px 10px 10px;
  overflow: auto;
`;

const Grid = styled("div")`
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

interface Tree {
  id: string;
  name: string;
  negation?: boolean;
  dateRestriction?: DateRangeT;
  children?: {
    connection: "and" | "or" | "time";
    direction: "horizontal" | "vertical";
    items: Tree[];
  };
}

const useEditorState = () => {
  const [tree, setTree] = useState<Tree | undefined>(undefined);

  const expandNode = (
    queryNode:
      | AndNodeT
      | DateRestrictionNodeT
      | OrNodeT
      | NegationNodeT
      | QueryConceptNodeT
      | SavedQueryNodeT,
    config: {
      negation?: boolean;
      dateRestriction?: DateRangeT;
    } = {},
  ): Tree => {
    switch (queryNode.type) {
      case "AND":
        return {
          id: "AND",
          name: "AND",
          ...config,
          children: {
            connection: "and",
            direction: "horizontal",
            items: queryNode.children.map((child) => expandNode(child)),
          },
        };
      case "OR":
        return {
          id: "OR",
          name: "OR",
          ...config,
          children: {
            connection: "or",
            direction: "vertical",
            items: queryNode.children.map((child) => expandNode(child)),
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
          id: queryNode.ids[0],
          name: concept?.label || "Unknown",
          ...config,
        };
      case "SAVED_QUERY":
        return {
          id: queryNode.query,
          name: queryNode.query,
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

  return {
    expandQuery,
    tree,
  };
};

const DROP_TYPES = [
  DNDType.PREVIOUS_QUERY,
  DNDType.PREVIOUS_SECONDARY_ID_QUERY,
];

export function EditorV2() {
  const { tree, expandQuery } = useEditorState();

  return (
    <Root>
      <Grid>
        {tree ? (
          <TreeNode
            tree={tree}
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

const Node = styled("div")<{ negated?: boolean; leaf?: boolean }>`
  padding: ${({ leaf }) => (leaf ? "5px 10px" : "10px")};
  border: 1px solid
    ${({ negated, theme, leaf }) =>
      negated ? "red" : leaf ? "black" : theme.col.grayMediumLight};
  border-radius: ${({ theme }) => theme.borderRadius};
  width: ${({ leaf }) => (leaf ? "150px" : "inherit")};
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
  border-width: 1px;
`;

const InvisibleDropzone = (
  props: Omit<DropzoneProps<any>, "acceptedDropTypes">,
) => {
  return (
    <InvisibleDropzoneContainer
      bare
      transparent
      acceptedDropTypes={[DNDType.CONCEPT_TREE_NODE]}
      {...props}
    />
  );
};

function TreeNode({
  tree,
  droppable,
}: {
  tree: Tree;
  droppable: {
    h: boolean;
    v: boolean;
  };
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
        <Node negated={tree.negation} leaf={!tree.children}>
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
                    <>
                      <Connector>{tree.children?.connection}</Connector>
                      <InvisibleDropzone
                        onDrop={(item) => {
                          console.log(item);
                        }}
                      />
                    </>
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
