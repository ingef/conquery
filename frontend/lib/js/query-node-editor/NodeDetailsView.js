// @flow

import React              from 'react';
import classnames         from 'classnames';
import T                  from 'i18n-react';
import { DropTarget }     from 'react-dnd';
import { dndTypes }       from '../common/constants';

import {
  AdditionalInfoHoverable
}                         from '../tooltip';
import { EditableText }   from '../form-components';
import { IconButton }     from '../button';

import { getConceptById } from '../category-trees/globalTreeStoreHelper';

import type { PropsType } from './QueryNodeEditor';

const ConceptEntry = AdditionalInfoHoverable(
  ({ node, canRemoveConcepts, onRemoveConcept, conceptId }) => (
    <div className="query-node-editor__concept">
      <div>
        <h6>{node.label}</h6>
        {
          node.description &&
          <p>{node.description}</p>
        }
      </div>
      {
        canRemoveConcepts &&
        <IconButton
          onClick={() => onRemoveConcept(conceptId)}
          className="btn--small btn--transparent"
          iconClassName="fa-trash-o"
        />
      }
    </div>
  )
);

const dropzoneTarget = {
  drop(props, monitor) {
    const item = monitor.getItem();
    props.onDropConcept(item);
  },

  canDrop({ node }, monitor) {
    const item = monitor.getItem();
    // The dragged item should contain exactly one id
    // since it was dragged from the tree
    const conceptId = item.ids[0];
    return item.tree === node.tree && !node.ids.some(id => id === conceptId);
  }
};

const collect = (connect, monitor) => ({
  connectDropTarget: connect.dropTarget(),
  isOver: monitor.isOver(),
  canDrop: monitor.canDrop(),
});

const ConceptDropzone =
  DropTarget(dndTypes.CATEGORY_TREE_NODE, dropzoneTarget, collect)(
    (props) => props.connectDropTarget(
      <div className="query-editor-dropzone">
        <div className={classnames(
          'dropzone', {
            'dropzone--over': props.isOver && props.canDrop,
            'dropzone--disallowed': props.isOver && !props.canDrop,
          }
        )}>
          <p className="dropzone__text">
            {T.translate('queryNodeEditor.dropConcept')}
          </p>
        </div>
      </div>
    )
);

export const NodeDetailsView = (props: PropsType) => {
  const { node, editorState } = props;

  return (
    <div className="query-node-editor__large_column query-node-editor__column">
      <h4>
        {
          !node.isPreviousQuery &&
          <EditableText
            loading={false}
            text={node.label}
            selectTextOnMount={true}
            editing={editorState.editingLabel}
            onSubmit={(value) => { props.onUpdateLabel(value); editorState.onToggleEditLabel(); }}
            onToggleEdit={editorState.onToggleEditLabel}
          />
        }
        {
          node.isPreviousQuery &&
          (node.label || node.id || node.ids)
        }
      </h4>
      <div className="query-node-editor__column_content">
        {
          props.isExcludeTimestampsPossible &&
          <div className="query-node-editor__row">
            <button
              type="button"
              className="btn btn--header-transparent"
              onClick={() => props.onToggleTimestamps(!node.excludeTimestamps)}
            >
              <i className={classnames('fa',
                {
                  'fa-square-o': !node.excludeTimestamps,
                  'fa-check-square-o': node.excludeTimestamps
                }
              )} /> {T.translate('queryNodeEditor.excludeTimestamps')}
            </button>
          </div>
        }
        {
          !node.isPreviousQuery &&
          <div className="query-node-editor__row">
            <h5>{[getConceptById(node.tree).label]}</h5>
            <div>
              <ConceptDropzone node={node} onDropConcept={props.onDropConcept} />
            </div>
            <div>
              {
                node.ids.map(conceptId => (
                  <ConceptEntry
                    key={conceptId}
                    node={getConceptById(conceptId)}
                    canRemoveConcepts={node.ids.length > 1}
                    onRemoveConcept={props.onRemoveConcept}
                    conceptId={conceptId}
                  />
                ))
              }
            </div>
          </div>
        }
      </div>
    </div>
  );
}
