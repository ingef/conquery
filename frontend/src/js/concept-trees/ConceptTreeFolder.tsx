import React from "react";
import styled from "@emotion/styled";

import type { ConceptT, ConceptIdT } from "../api/types";

import { getConceptById } from "./globalTreeStoreHelper";
import type { SearchT } from "./reducer";

import Openable from "./Openable";
import ConceptTree from "./ConceptTree";
import ConceptTreeNodeTextContainer from "./ConceptTreeNodeTextContainer";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
`;

type PropsType = {
  depth: number;
  trees: Object;
  tree: ConceptT;
  treeId: ConceptIdT;
  active: boolean;
  open?: boolean;
  onToggleOpen?: Function;
  search: SearchT;
  onLoadTree: (id: string) => void;
};

const sumMatchingEntries = (children, initSum) => {
  return children.reduce((sum, treeId) => {
    const rootConcept = getConceptById(treeId);
    const rootMatchingEntries = rootConcept ? rootConcept.matchingEntries : 0;

    return rootMatchingEntries ? sum + rootMatchingEntries : sum;
  }, initSum);
};

const ConceptTreeFolder = (props: PropsType) => {
  const { tree, search } = props;
  const matchingEntries =
    !tree.children || !tree.matchingEntries
      ? null
      : sumMatchingEntries(tree.children, tree.matchingEntries);

  return (
    <Root>
      <ConceptTreeNodeTextContainer
        node={{
          id: props.treeId,
          label: props.tree.label,
          description: props.tree.description,
          matchingEntries: matchingEntries,
          dateRange: props.tree.dateRange,
          additionalInfos: props.tree.additionalInfos,
          children: props.tree.children
        }}
        createQueryElement={() => {
          // We don't have to implement this since ConceptTreeFolders should never be
          // dragged into the editor, hence they're 'active: false' and thus not draggable
        }}
        isStructFolder
        open={props.open || false}
        depth={props.depth}
        active={props.active}
        onTextClick={props.onToggleOpen}
        search={search}
      />
      {props.open &&
        props.tree.children &&
        props.tree.children.map((childId, i) => {
          const tree = props.trees[childId];

          const treeProps = {
            key: i,
            treeId: childId,
            depth: props.depth + 1,
            search,
            onLoadTree: props.onLoadTree
          };

          if (tree.detailsAvailable) {
            const rootConcept = getConceptById(childId);

            return (
              <ConceptTree
                id={childId}
                label={tree.label}
                description={tree.description}
                error={tree.error}
                loading={tree.loading}
                tree={rootConcept}
                {...treeProps}
              />
            );
          } else {
            const treeFolderProps = {
              tree,
              trees: props.trees,
              openInitially: false,
              active: tree.active
            };

            return tree.children &&
              props.tree.children &&
              props.tree.children.length > 0 ? (
              <OpenableConceptTreeFolder {...treeFolderProps} {...treeProps} />
            ) : (
              <ConceptTreeFolder {...treeFolderProps} {...treeProps} />
            );
          }
        })}
    </Root>
  );
};

const OpenableConceptTreeFolder = Openable(ConceptTreeFolder);

export default OpenableConceptTreeFolder;
