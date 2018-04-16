// @flow

import React                          from 'react';

import type {
  NodeType,
  TreeNodeIdType
}                                     from '../common/types/backend';

import { getConceptById }             from './globalTreeStoreHelper';
import Openable                       from './Openable';
import CategoryTree                   from './CategoryTree';
import CategoryTreeNodeTextContainer  from './CategoryTreeNodeTextContainer';
import { type SearchType }            from './reducer';

type PropsType = {
  depth: number,
  trees: Object,
  tree: NodeType,
  treeId: TreeNodeIdType,
  active: boolean,
  open?: boolean,
  onToggleOpen?: Function,
  search?: SearchType,
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

const isSearchResultInChildren = (children: TreeNodeIdType[], search?: SearchType) => {
  if (!search || !search.result) return false;
  const result = search.result;

  for (var i = 0; i < result.length; i++) {
    const id = result[i];
    for (var j = 0; j < children.length; j++) {
      const childId = children[j];
      if (id.indexOf(childId) >= 0)
        return true;
    }
  }
  return false;
}

const CategoryTreeFolder = (props: PropsType) => {
  const { tree, search } = props;
  const matchingEntries = !tree.children || !tree.matchingEntries
    ? null
    : sumMatchingEntries(tree.children, tree.matchingEntries);

  const searching = search && search.searching
  const render = searching && tree.children
  ? isSearchResultInChildren(tree.children, search)
  : true;
  return render && (
    <div className="category-tree-folder category-tree-node">
      <CategoryTreeNodeTextContainer
        node={{
          id: props.treeId,
          label: props.tree.label,
          description: props.tree.description,
          matchingEntries: matchingEntries,
          dateRange: props.tree.dateRange,
          additionalInfos: props.tree.additionalInfos,
          hasChildren: !!props.tree.children && props.tree.children.length > 0,
        }}
        createQueryElement={() => {
          // We don't have to implement this since CategoryTreeFolders should never be
          // dragged into the editor, hence they're 'active: false' and thus not draggable
        }}
        open={props.open || false}
        depth={props.depth}
        active={props.active}
        onTextClick={props.onToggleOpen}
      />
      {
        props.open && props.tree.children && props.tree.children.map((childId, i) => {
          const tree = props.trees[childId];

          if (tree.detailsAvailable) {
            const rootConcept = getConceptById(childId);

            return (
              <CategoryTree
                key={i}
                id={childId}
                label={tree.label}
                tree={rootConcept}
                treeId={childId}
                loading={tree.loading}
                error={tree.error}
                depth={props.depth + 1}
                search={search}
              />
            );
          } else {
            return tree.children && props.tree.children && props.tree.children.length > 0
              ? <OpenableCategoryTreeFolder
                  key={i}
                  trees={props.trees}
                  tree={tree}
                  treeId={childId}
                  openInitially={false}
                  depth={props.depth + 1}
                  active={tree.active}
                />
              : <CategoryTreeFolder
                  key={i}
                  trees={props.trees}
                  tree={tree}
                  treeId={childId}
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
