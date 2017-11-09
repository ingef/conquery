// @flow

import React                   from 'react';
import T                       from 'i18n-react';

import QueryEditorDropzone     from './QueryEditorDropzone';
import QueryNode               from './QueryNode';
import QueryGroupActions       from './QueryGroupActions';
import type { QueryGroupType } from './types';

type PropsType = {
  group: QueryGroupType,
  andIdx: number,
  onDropNode: Function,
  onDeleteNode: Function,
  onFilterClick: Function,
  onExcludeClick: Function,
  onExpandClick: Function,
  onDateClick: Function,
  onDeleteGroup: Function,
  onLoadPreviousQuery: Function,
};

const QueryGroup = (props: PropsType) => {
  const dateActiveClass = props.group.dateRange
    ? 'query-group__action--active'
    : '';
  const excludeActiveClass = props.group.exclude
    ? 'query-group__action--active'
    : '';
  const groupExcludeActiveClass = props.group.exclude
    ? 'query-group__group--active'
    : '';

  return (
    <div className="query-group">
      <QueryEditorDropzone
        key={props.group.elements.length + 1}
        onDropNode={props.onDropNode}
        onLoadPreviousQuery={props.onLoadPreviousQuery}
      />
      <p className="query-or-connector">{T.translate('common.or')}</p>
      <div className={`query-group__group ${groupExcludeActiveClass}`}>
        <QueryGroupActions
          excludeActiveClass={excludeActiveClass}
          dateActiveClass={dateActiveClass}
          onExcludeClick={props.onExcludeClick}
          onDeleteGroup={props.onDeleteGroup}
          onDateClick={props.onDateClick}
        />
        {
          props.group.elements.map((node, orIdx) => (
            [
              <QueryNode
                key={orIdx}
                node={node}
                andIdx={props.andIdx}
                orIdx={orIdx}
                onDeleteNode={() => props.onDeleteNode(orIdx)}
                onFilterClick={() => props.onFilterClick(orIdx)}
                onExpandClick={props.onExpandClick}
              />,
              orIdx !== props.group.elements.length - 1
                ? <p className="query-or-connector">{T.translate('common.or')}</p>
                : '' // Render last OR outside of this frame
            ]
          ))
        }
      </div>
    </div>
  );
};

export default QueryGroup;
