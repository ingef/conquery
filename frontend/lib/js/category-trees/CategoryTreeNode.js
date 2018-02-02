// @flow

import React                         from 'react';

import { getConceptById }            from './globalTreeStoreHelper';

import Openable                      from './Openable';
import CategoryTreeNodeTextContainer from './CategoryTreeNodeTextContainer';

type PropsType = {
  id: string | number,
  data: Object,
  depth: number,
  open: boolean,
  onToggleOpen: Function,
};

class CategoryTreeNode extends React.Component {
  props: PropsType;

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
            tables: data.tables,
            matchingEntries: data.matchingEntries,
            dateRange: data.dateRange,
            additionalInfos: data.additionalInfos,
            hasChildren: !!data.children,
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

                const childWithTables = {
                  ...child,
                  tables: data.tables
                };

                return (
                  <OpenableCategoryTreeNode
                    key={i}
                    id={childId}
                    data={childWithTables}
                    depth={this.props.depth + 1}
                  />
                );
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
