// @flow

import React                from 'react';
import T                    from 'i18n-react';

import { ErrorMessage }     from '../error-message';

import CategoryTreeNode     from './CategoryTreeNode';

import type { TreeNodeType }     from './reducer';

type PropsType = {
  id: string | number,
  tree: ?TreeNodeType,
  label: string,
  depth: number,
  loading: boolean,
  error: ?string,
};

const CategoryTree = (props: PropsType) => {
  if (props.loading)
    return (
      <p className="category-tree-list__loading-tree">
        <span className="category-tree-list__loading-tree__spinner">
          <i className="fa fa-spinner" />
        </span>
        <span>
          { T.translate('categoryTreeList.loading', { tree: props.label }) }
        </span>
      </p>
    );
  else if (props.error)
    return (
      <ErrorMessage
        className="category-tree-list__error-tree"
        message={T.translate('categoryTreeList.error', { tree: props.label })}
      />
    );
  else if (props.tree)
    return (
      <div className="category-tree">
        <CategoryTreeNode
          id={props.id}
          data={props.tree}
          depth={props.depth}
        />
      </div>
    );
  else return null;
};

export default CategoryTree;
