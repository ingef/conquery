// @flow

import React from "react";
import styled from "@emotion/styled";

import type { ConceptT, ConceptIdT } from "../api/types";

import { getConceptById } from "./globalTreeStoreHelper";
import { type SearchType } from "./reducer";

import Openable from "./Openable";
import ConceptTree from "./ConceptTree";
import ConceptTreeNodeTextContainer from "./ConceptTreeNodeTextContainer";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
`;

type PropsType = {
  depth: number,
  trees: Object,
  tree: ConceptT,
  treeId: ConceptIdT,
  active: boolean,
  open?: boolean,
  onToggleOpen?: Function,
  search: SearchType,
  onLoadTree: (id: string) => void
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
        isTreeFolder
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

          if (tree.detailsAvailable) {
            const rootConcept = getConceptById(childId);

            return (
              <ConceptTree
                key={i}
                id={childId}
                label={tree.label}
                tree={rootConcept}
                treeId={childId}
                loading={tree.loading}
                error={tree.error}
                depth={props.depth + 1}
                search={search}
                onLoadTree={props.onLoadTree}
              />
            );
          } else {
            return tree.children &&
              props.tree.children &&
              props.tree.children.length > 0 ? (
              <OpenableConceptTreeFolder
                key={i}
                trees={props.trees}
                tree={tree}
                treeId={childId}
                openInitially={false}
                depth={props.depth + 1}
                active={tree.active}
                search={search}
                onLoadTree={props.onLoadTree}
              />
            ) : (
              <ConceptTreeFolder
                key={i}
                trees={props.trees}
                tree={tree}
                treeId={childId}
                openInitially={false}
                depth={props.depth + 1}
                active={tree.active}
                search={search}
                onLoadTree={props.onLoadTree}
              />
            );
          }
        })}
    </Root>
  );
};

const OpenableConceptTreeFolder = Openable(ConceptTreeFolder);

export default OpenableConceptTreeFolder;
