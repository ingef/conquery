// @flow

import React                     from 'react';
import T                         from 'i18n-react';
import classnames                from 'classnames';

import { tableHasActiveFilters } from '../model/table';
import { nodeHasActiveFilters }  from '../model/node';

import type { PropsType }        from './QueryNodeEditor';

export const MenuColumn = (props: PropsType) => {
  const { node, editorState } = props;

  const onlyOneTableIncluded = node.tables.filter(table => !table.exclude).length === 1;
  const allowToggleTables = node.tables.map(table => table.exclude || !onlyOneTableIncluded);

  return (
    <div className="query-node-editor__fixed_column query-node-editor__column">
      <div className="query-node-editor__category_header">
        {T.translate('queryNodeEditor.queryNode')}
      </div>
      <button
        className={classnames(
          'query-node-editor__category_element',
          'btn', 'btn--header-transparent',
          { 'query-node-editor__category_element_active': editorState.detailsViewActive })}
        onClick={(e) => { e.preventDefault(); editorState.onSelectDetailsView() }}
      >
        {node.label}
      </button>
      {
        props.showTables &&
        <div>
          <div className="query-node-editor__category_header">
            {T.translate('queryNodeEditor.conceptNodeTables')}
          </div>
          {node.tables.map((table, tableIdx) => (
            <div
              key={tableIdx}
              className={classnames(
                'query-node-editor__category_element',
                'btn',
                'btn--header-transparent',
                {
                  'query-node-editor__category_element_active':
                    editorState.selectedInputTableIdx === tableIdx && !editorState.detailsViewActive
                }
              )}
              onClick={() => { editorState.onSelectInputTableView(tableIdx); }}
            >
              <i
                className={classnames(
                  'fa', {
                    'fa-square-o': !!table.exclude,
                    'fa-check-square-o': !table.exclude,
                    'query-node-editor__exclude_icon': allowToggleTables[tableIdx],
                    'query-node-editor__exclude_icon_disabled': !allowToggleTables[tableIdx],
                  }
                )}
                onClick={event => {
                  event.stopPropagation();
                  if (allowToggleTables[tableIdx])
                    props.onToggleTable(tableIdx, !table.exclude);
                }}
              />
              <span>{table.label}</span>
              {
                tableHasActiveFilters(table) &&
                <i className="fa fa-filter query-node-editor__filter_icon" />
              }
            </div>
          ))
          }
          {
            nodeHasActiveFilters(node) &&
            <div className="query-node-editor__category_action">
              <span
                className="query-node-editor__reset-all"
                onClick={() => props.onResetAllFilters()}
              >
                <i className="fa fa-undo" /> {T.translate('queryNodeEditor.resetAll')}
              </span>
            </div>
          }
        </div>
      }
    </div>
  )
}
