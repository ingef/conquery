// @flow

import React                        from 'react';
import { connect }                  from 'react-redux';
import T                            from 'i18n-react';

import type { StateType }           from '../app/reducers';
import { ErrorMessage }             from '../error-message';

import { getConceptById }           from './globalTreeStoreHelper';
import {
  type TreesType,
  type SearchType
}                                   from './reducer';
import CategoryTree                 from './CategoryTree';
import CategoryTreeFolder           from './CategoryTreeFolder';
import { isInSearchResult }         from './selectors';

type PropsType = {
  trees: TreesType,
  activeTab: string,
  search?: SearchType,
};

class CategoryTreeList extends React.Component<PropsType> {
  props: PropsType;

  shouldComponentUpdate(nextProps, nextState) {
    return nextProps.search.updateComponent;
  }

  render() {
    const { search } = this.props;
    const searching = search && search.searching

    return !search.loading && (
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
          this.props.trees
            ? Object
                .keys(this.props.trees)
                // Only take those that don't have a parent, they must be root
                .filter(treeId => !this.props.trees[treeId].parent)
                .map((treeId, i) => {
                  const tree = this.props.trees[treeId];
                  const rootConcept = getConceptById(treeId);

                  const render = searching
                  ? isInSearchResult(treeId, tree.children, search)
                  : true;

                  if (!render) return null;

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
                        search={this.props.search}
                      />
                    : <CategoryTreeFolder
                        key={i}
                        trees={this.props.trees}
                        tree={tree}
                        treeId={treeId}
                        depth={0}
                        active={tree.active}
                        openInitially
                        search={this.props.search}
                      />;
                })
          : <ErrorMessage
              className="category-tree-list__error-tree"
              message={T.translate('categoryTreeList.noTrees')}
            />
        }
      </div>
    );
  }
}

const mapStateToProps = (state: StateType) => {
  return {
    trees: state.categoryTrees.trees,
    activeTab: state.panes.left.activeTab,
    search: state.categoryTrees.search
  };
};

export default connect(mapStateToProps)(CategoryTreeList);
