// @flow

import React                         from 'react';

import {
  type TreeNodeIdType,
  type InfoType,
  type DateRangeType,
  type NodeType,
  type SearchType
}                                    from '../common/types/backend';

import { type DraggedNodeType }      from '../standard-query-editor/types';

import { getConceptById }            from './globalTreeStoreHelper';

import Openable                      from './Openable';
import CategoryTreeNodeTextContainer from './CategoryTreeNodeTextContainer';

// Concept data that is necessary to display tree nodes. Includes additional infos
// for the tooltip as well as the id of the corresponding tree
type TreeNodeData = {
  label: string,
  description: string,
  active: boolean,
  matchingEntries: number,
  dateRange: DateRangeType,
  additionalInfos: Array<InfoType>,
  children: Array<TreeNodeIdType>,

  tree: TreeNodeIdType,
}

type PropsType = {
  id: TreeNodeIdType,
  data: TreeNodeData,
  depth: number,
  open: boolean,
  search: SearchType,
  onToggleOpen: () => void,
};

const selectTreeNodeData = (concept: NodeType, tree: TreeNodeIdType) => ({
  label: concept.label,
  description: concept.description,
  active: concept.active,
  matchingEntries: concept.matchingEntries,
  dateRange: concept.dateRange,
  additionalInfos: concept.additionalInfos,
  children: concept.children,

  tree,
});

class CategoryTreeNode extends React.Component<PropsType> {
  _onToggleOpen() {
    if (!this.props.data.children) return;

    this.props.onToggleOpen();
  }

  _matchedSearch(treeId, search: SearchType, searchCurTree: []): boolean {
    if (search && search.words.length > 0)
      return Object.keys(search.result).some(key => search.result[key].includes(treeId))
    return true;
  }

  render() {
    const { id, data, depth, open, search } = this.props;
    const searching = search && search.words.length > 0;

    return this._matchedSearch(id, search) && (
      <div className="category-tree-node">
        <CategoryTreeNodeTextContainer
          node={{
            id,
            label: data.label,
            description: data.description,
            matchingEntries: data.matchingEntries,
            dateRange: data.dateRange,
            additionalInfos: data.additionalInfos,
            hasChildren: !!data.children && data.children.length > 0,

          }}
          createQueryElement={() : DraggedNodeType => {
            const tables = getConceptById(data.tree).tables;
            return {
              ids: [id],
              label: data.label,
              tables: tables,
              tree: data.tree
            };
          }}
          open={open}
          depth={depth}
          active={data.active}
          onTextClick={this._onToggleOpen.bind(this)}
          search={search}
        />
        {
          !!data.children && (open || searching) &&
          <div className="category-tree-node__children">
            {
              data.children.map((childId, i) => {
                const child = getConceptById(childId);

                return child ? (
                  <OpenableCategoryTreeNode
                    key={i}
                    id={childId}
                    data={selectTreeNodeData(child, data.tree)}
                    depth={this.props.depth + 1}
                    search={this.props.search}
                  />
                ) : null;
              })
            }
          </div>
        }
      </div>
    );
  }
};

const OpenableCategoryTreeNode = Openable(CategoryTreeNode);

export default OpenableCategoryTreeNode;
