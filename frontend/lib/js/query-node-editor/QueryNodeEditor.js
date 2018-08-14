// @flow

import React                            from 'react';
import type { Dispatch }                from 'redux-thunk';
import { connect }                      from 'react-redux';
import T                                from 'i18n-react';

import { type QueryNodeType }           from '../standard-query-editor/types';

import { MenuColumn }                   from './MenuColumn';
import { NodeDetailsView }              from './NodeDetailsView';
import { TableFilterView }              from './TableFilterView';
import { DescriptionColumn }            from './DescriptionColumn';

import { createQueryNodeEditorActions } from './actions';

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
  node: QueryNodeType,
  showTables: boolean,
  isExcludeTimestampsPossible: boolean,
  onCloseModal: Function,
  onUpdateLabel: Function,
  onDropConcept: Function,
  onRemoveConcept: Function,
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
  const { node, editorState } = props;

  if (!node) return null;

  const selectedTable = !node.isPreviousQuery && editorState.selectedInputTableIdx != null
    ? node.tables[editorState.selectedInputTableIdx]
    : null;

  return (
      <div className="query-node-editor">
        <div className="query-node-editor__wrapper">
          <MenuColumn {...props} />
          {
            editorState.detailsViewActive &&
            <NodeDetailsView {...props} />
          }
          {
            !editorState.detailsViewActive && selectedTable != null &&
            <TableFilterView {...props} />
          }
          {
            !editorState.detailsViewActive &&
            <DescriptionColumn {...props} />
          }
          <button
            type="button"
            className="query-node-editor__close-button btn btn--transparent btn--small"
            onClick={() => { editorState.onReset(); props.onCloseModal() }}
          >
            { T.translate('common.done') }
          </button>
        </div>
      </div>
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
      reset
    } = createQueryNodeEditorActions(ownProps.type);

    return {
      ...externalDispatchProps,
      editorState: {
        ...(externalDispatchProps.editorState || {}),
        onSelectDetailsView: () => dispatch(setDetailsViewActive()),
        onToggleEditLabel: () => dispatch(toggleEditLabel()),
        onSelectInputTableView: (tableIdx) => dispatch(setInputTableViewActive(tableIdx)),
        onShowDescription: (filterIdx) => dispatch(setFocusedInput(filterIdx)),
        onReset: () => dispatch(reset())
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
        ...(dispatchProps.editorState || {})
      }
    };
  };

  return connect(mapStateToProps, mapDispatchToPropsInternal, mergePropsInternal)(QueryNodeEditor);
};
