// @flow

import React                         from 'react';

<<<<<<< HEAD
import { getConceptById }            from './globalTreeStoreHelper';
=======
import {
  type TreeNodeIdType,
  type InfoType,
  type DateRangeType,
  type NodeType
}                                    from '../common/types/backend';
import { type DraggedNodeType, type ConceptType, type TableType } from '../standard-query-editor/types';

import { getConceptById } from './globalTreeStoreHelper';
>>>>>>> d25d186... Refactor types around ConceptTrees, DnD, Standard Editor Nodes

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

// Converts a tree item into a concept that will be used as part of a Query Node in the editor
const conceptFromTreeNodeData = (conceptId: TreeNodeIdType, treeItem: TreeNodeData): ConceptType => ({
  id: conceptId,
  label: treeItem.label,
  description: treeItem.description,
  matchingEntries: treeItem.matchingEntries,
  dateRange: treeItem.dateRange,
  additionalInfos: treeItem.additionalInfos,
  hasChildren: !!treeItem.children,
});

// Creates node from a given concept that can be dragged on the editor
const createNode = (concept: ConceptType, label: string, tables: Array<TableType>, tree: TreeNodeIdType) : DraggedNodeType => ({
  ids: [concept.id],
  label: concept.label,
  tables: tables,
  tree,
  concepts: [concept]
});

class CategoryTreeNode extends React.Component<PropsType> {
  _onToggleOpen() {
    if (!this.props.data.children) return;

    this.props.onToggleOpen();
  }

  render() {
    const { id, data, depth, open } = this.props;

    return (
      <div className="category-tree-node">
        <CategoryTreeNodeTextContainer
          node={{
            id,
            label: data.label,
            description: data.description,
            matchingEntries: data.matchingEntries,
            dateRange: data.dateRange,
            additionalInfos: data.additionalInfos,
            hasChildren: !!data.children,
          }}
          createQueryElement={() => {
            const concept = conceptFromTreeNodeData(id, data);
            const tables = getConceptById(data.tree).tables;
            return createNode(concept, concept.label, tables, data.tree);
          }}
          open={open}
          depth={depth}
          active={data.active}
          onTextClick={this._onToggleOpen.bind(this)}
        />
        {
          !!data.children && open &&
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
