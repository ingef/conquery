// @flow

import React                from 'react';
import type { Dispatch }    from 'redux-thunk';
import { connect }          from 'react-redux';
import T                    from 'i18n-react';
import classnames           from 'classnames';

import { Modal }            from '../modal';
import ParameterTable       from './ParameterTable';
import {
  createQueryNodeEditorActions
}                           from './actions';

type QueryNodeEditorState = {
  detailsViewActive: boolean,
  selectedInputTableIdx: number,
  selectedInput: number,
  editingLabel: boolean,
  onSelectDetailsView: Function,
  onSelectInputTableView: Function,
  onShowDescription: Function,
  onToggleEditLabel: Function,
  onReset: Function,
  onDropFiles: Function,
}

export type PropsType = {
  name: string,
  editorState: QueryNodeEditorState,
  node: ?Object,
  showTables: boolean,
  isExcludeTimestampsPossible: boolean,
  onCloseModal: Function,
  onToggleTable: Function,
  onSetFilterValue: Function,
  onResetAllFilters: Function,
  onToggleTimestamps: Function,
  onSwitchFilterMode: Function,
  onLoadFilterSuggestions: Function,
  datasetId: number,
  suggestions: ?Object,
};

const QueryNodeEditor = (props: PropsType) => {
  const {
    node,
    // In the future: use QueryNodeEditor-internal state from here
    // editorState
  } = props;

  if (!node) return null;

  return (
    <Modal closeModal={props.onCloseModal} doneButton>
      <div className="query-node-editor">
        <h3 className="query-node-editor__headline">{node.label}</h3>
        {
          node.description &&
          <p className="query-node-editor__description">{node.description}</p>
        }
        <p className="query-node-editor__explanation">
          { T.translate('queryNodeEditor.explanation') }
          {
            node.hasActiveFilters &&
            <span
              className="query-node-editor__reset-all"
              onClick={props.onResetAllFilters}
            >
              <i className="fa fa-undo" /> {T.translate('queryNodeEditor.resetAll')}
            </span>
          }
        </p>
        {
          props.isExcludeTimestampsPossible &&
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
        }
        <div className="query-node-editor__tables">
          {
            props.showTables && node.tables.map((table, tableIdx) => (
              <ParameterTable
                table={table}
                key={tableIdx}
                allowToggleTable={node.tables.length > 1}
                onToggleTable={() => props.onToggleTable(
                  tableIdx,
                  !table.exclude
                )}
                onSetFilterValue={(filterIdx, value) => props.onSetFilterValue(
                  tableIdx,
                  filterIdx,
                  value
                )}
                onSwitchFilterMode={(filterIdx, mode) => props.onSwitchFilterMode(
                  tableIdx,
                  filterIdx,
                  mode
                )}
                onLoadFilterSuggestions={(filterIdx, filterId, prefix) =>
                  props.onLoadFilterSuggestions(
                    props.datasetId,
                    tableIdx,
                    node.tables[tableIdx].id,
                    node.id,
                    filterIdx,
                    filterId,
                    prefix
                )}
                suggestions={props.suggestions && props.suggestions[tableIdx]}
              />
            ))
          }
        </div>
      </div>
    </Modal>
  );
};

export const createConnectedQueryNodeEditor = (
  mapStateToProps: Function,
  mapDispatchToProps: Function,
  mergeProps: Function
) => {
  const mapDispatchToPropsInternal = (dispatch: Dispatch, ownProps) => {
    const externalDispatchProps = mapDispatchToProps ? mapDispatchToProps(dispatch, ownProps) : {};

    const {
      setDetailsViewActive,
      toggleEditLabel,
      setInputTableViewActive,
      setFocusedInput,
      reset,
      onDropFiles
    } = createQueryNodeEditorActions(ownProps.type);

    return {
      ...externalDispatchProps,
      editorState: {
        ...(externalDispatchProps.editorState || {}),
        onSelectDetailsView: () => dispatch(setDetailsViewActive()),
        onToggleEditLabel: () => dispatch(toggleEditLabel()),
        onSelectInputTableView: (tableIdx) => dispatch(setInputTableViewActive(tableIdx)),
        onShowDescription: (filterIdx) => dispatch(setFocusedInput(filterIdx)),
        onReset: () => dispatch(reset()),
        onDropFiles: (...params) => dispatch(onDropFiles(...params))
      }
    };
  }

  const mergePropsInternal = (stateProps, dispatchProps, ownProps) => {
    const externalMergedProps = mergeProps
      ? mergeProps(stateProps, dispatchProps, ownProps)
      : { ...ownProps, ...stateProps, ...dispatchProps };

    return {
      ...externalMergedProps,
      editorState: {
        ...(stateProps.editorState || {}),
        ...(dispatchProps.editorState || {}),
        onDropFiles: (...params) => dispatchProps.editorState.onDropFiles(...params)
      }
    };
  };

  return connect(mapStateToProps, mapDispatchToPropsInternal, mergePropsInternal)(QueryNodeEditor);
};
