// @flow

import React                from 'react';
import { connect }          from 'react-redux';

import type { StateType }   from '../app/reducers';

import { getConceptById }   from './globalTreeStoreHelper';

import { type TreesType }   from './reducer';

import CategoryTree         from './CategoryTree';
import CategoryTreeFolder   from './CategoryTreeFolder';

type PropsType = {
  trees: TreesType,
  activeTab: string,
};

class CategoryTreeList extends React.Component<PropsType> {
  props: PropsType;

  render() {
    return (
      <div className="category-tree-list" style={{
        // Only hide the category trees when the tab is not selected
        // Because mount / unmount would reset the open states
        // that are React states and not part of the Redux state
        // because if they were part of Redux state, the entire tree
        // would have to re-render when a single node would be opened
        //
        // Also: Can't set it to initial, because IE11 doesn't work then
        // => Empty string instead
        display: this.props.activeTab !== 'categoryTrees' ? 'none' : ''
      }}>
        {
          Object
            .keys(this.props.trees)
            // Only take those that don't have a parent, they must be root
            .filter(treeId => !this.props.trees[treeId].parent)
            .map((treeId, i) => {
              const tree = this.props.trees[treeId];
              const rootConcept = getConceptById(treeId);

              return tree.detailsAvailable
                ? <CategoryTree
                    key={i}
                    id={treeId}
                    label={tree.label}
                    tree={rootConcept}
                    treeId={treeId}
                    loading={!!tree.loading}
                    error={tree.error}
                    depth={0}
                  />
                : <CategoryTreeFolder
                    key={i}
                    trees={this.props.trees}
                    tree={tree}
                    treeId={treeId}
                    depth={0}
                    active={tree.active}
                    openInitially
                  />;
            })
        }
      </div>
    );
  }
}

const mapStateToProps = (state: StateType) => {
  return {
    trees: state.categoryTrees.trees,
    activeTab: state.panes.left.activeTab,
  };
};

export default connect(mapStateToProps)(CategoryTreeList);
