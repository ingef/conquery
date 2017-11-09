// @flow

import React                         from 'react';

import { getConceptById }            from './globalTreeStoreHelper';

import Openable                      from './Openable';
import CategoryTree                  from './CategoryTree';
import CategoryTreeNodeTextContainer from './CategoryTreeNodeTextContainer';

import {
  type TreeNodeType,
  type TreeNodeIdType,
} from './reducer';

type PropsType = {
  depth: number,
  trees: Object,
  tree: TreeNodeType,
  treeId: TreeNodeIdType,
  active: boolean,
  open?: boolean,
  onToggleOpen?: Function,
};

const sumMatchingEntries = (children, initSum) => {
  return children.reduce((sum, treeId) => {
    const rootConcept = getConceptById(treeId);
    const rootMatchingEntries = rootConcept ? rootConcept.matchingEntries : 0;

    return rootMatchingEntries
      ? sum + rootMatchingEntries
      : sum;
  }, initSum);
};

const CategoryTreeFolder = (props: PropsType) => {
  const matchingEntries = !props.tree.children || !props.tree.matchingEntries
    ? null
    : sumMatchingEntries(props.tree.children, props.tree.matchingEntries);

  return (
    <div className="category-tree-folder category-tree-node">
      <CategoryTreeNodeTextContainer
        node={{
          id: props.treeId,
          label: props.tree.label,
          description: props.tree.description,
          tables: props.tree.tables,
          matchingEntries: matchingEntries,
          additionalInfos: props.tree.additionalInfos,
          hasChildren: !!props.tree.children && props.tree.children.length > 0,
        }}
        open={props.open || false}
        depth={props.depth}
        active={props.active}
        onTextClick={props.onToggleOpen}
      />
      {
        props.open && props.tree.children && props.tree.children.map((treeId, i) => {
          const tree = props.trees[treeId];

          if (tree.detailsAvailable) {
            const rootConcept = getConceptById(treeId);

            return (
              <CategoryTree
                key={i}
                id={treeId}
                label={tree.label}
                tree={rootConcept}
                loading={tree.loading}
                error={tree.error}
                depth={props.depth + 1}
              />
            );
          } else {
            return tree.children && props.tree.children && props.tree.children.length > 0
              ? <OpenableCategoryTreeFolder
                  key={i}
                  trees={props.trees}
                  tree={tree}
                  treeId={treeId}
                  openInitially={false}
                  depth={props.depth + 1}
                  active={tree.active}
                />
              : <CategoryTreeFolder
                  key={i}
                  trees={props.trees}
                  tree={tree}
                  treeId={treeId}
                  openInitially={false}
                  depth={props.depth + 1}
                  active={tree.active}
                />;
          }
        })
      }
    </div>
  );
};


const OpenableCategoryTreeFolder = Openable(CategoryTreeFolder);

export default OpenableCategoryTreeFolder;
