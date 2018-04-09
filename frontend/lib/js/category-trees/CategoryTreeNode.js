// @flow

import React                          from 'react';
import { getConceptById }             from './globalTreeStoreHelper';
import Openable                       from './Openable';
import CategoryTreeNodeTextContainer  from './CategoryTreeNodeTextContainer';
import { type SearchType }            from './reducer';

type PropsType = {
  id: string | number,
  data: Object,
  depth: number,
  open: boolean,
  onToggleOpen: Function,
  search: SearchType
};

class CategoryTreeNode extends React.Component {
  props: PropsType;

  _onToggleOpen() {
    if (!this.props.data.children) return;

    this.props.onToggleOpen();
  }

  _matchedSearch(treeId, search: SearchType, searchCurTree: []): boolean {
    if (search.words.length > 0)
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
          search={search}
        />
        {
          !!data.children && (open || searching) &&
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
                    search={this.props.search}
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
