// @flow

import React              from 'react';
import classnames         from 'classnames';
import T                  from 'i18n-react';

import { EditableText }   from '../form-components';
import { ScrollableList } from '../scrollable-list';

import type { PropsType } from './QueryNodeEditor';

export const NodeDetailsView = (props: PropsType) => {
  const { node, editorState } = props;

  return (
    <div>
      <h4>
        <EditableText
          loading={false}
          text={node.label}
          selectTextOnMount={true}
          editing={editorState.editingLabel}
          onSubmit={(value) => { props.onUpdateLabel(value); editorState.onToggleEditLabel(); }}
          onToggleEdit={editorState.onToggleEditLabel}
        />
      </h4>
      <div className="query-node-editor__column_content">
        {
          props.isExcludeTimestampsPossible &&
          <div className="query-node-editor__row">
            <button
              type="button"
              className="query-node-editor__toggle-timestamps btn btn--header-transparent"
              onClick={() => props.onToggleTimestamps(!node.excludeTimestamps)}
            >
              <i className={classnames(
                'parameter-table__exclude-icon',
                'fa',
                {
                  'fa-square-o': !node.excludeTimestamps,
                  'fa-check-square-o': node.excludeTimestamps
                }
              )} /> {T.translate('queryNodeEditor.excludeTimestamps')}
            </button>
          </div>
        }

        <div className="query-node-editor__row">
          <label className={classnames('input')}>
            <span className={classnames('input-label')}>
              {T.translate('queryNodeEditor.conceptTree')}
            </span>
            <ScrollableList items={[node.tree.label]} />
          </label>
        </div>
        <div className="query-node-editor__row">
          <label className={classnames('input')}>
            <span className={classnames('input-label')}>
              {T.translate('queryNodeEditor.conceptNodes')}
            </span>
            <ScrollableList
              items={node.concepts.map(x => x.description ? `${x.label - x.description}` : x.label)}
            />
          </label>
        </div>
      </div>
    </div>
  );
}
