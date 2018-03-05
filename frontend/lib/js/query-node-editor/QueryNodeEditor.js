// @flow

import React                 from 'react';
import type { Dispatch }     from 'redux-thunk';
import { connect }           from 'react-redux';
import T                     from 'i18n-react';
import classnames            from 'classnames';

import { EditableText }      from '../form-components';
import { ScrollableList }    from '../scrollable-list';
import { Modal }             from '../modal';
import { isEmpty }           from '../common/helpers';
import ParameterTableFilters from './ParameterTableFilters';

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
}

export type PropsType = {
  name: string,
  editorState: QueryNodeEditorState,
  node: ?Object,
  showTables: boolean,
  isExcludeTimestampsPossible: boolean,
  onCloseModal: Function,
  onUpdateLabel: Function,
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

  const selectedTable = editorState.selectedInputTableIdx != null ? node.tables[editorState.selectedInputTableIdx] : null;
  const onlyOneTableIncluded = node.tables.filter(table => !table.exclude).length === 1;
  const allowToggleTables = node.tables.map(table => table.exclude || !onlyOneTableIncluded);

  return (
    <Modal closeModal={props.onCloseModal} doneButton>
      <div className="query-node-editor">
        <div className="query-node-editor__fixed_column query-node-editor__column">
          <div className="query-node-editor__category_header">
            {T.translate('queryNodeEditor.conceptNode')}
          </div>
          <button
            className={classnames(
              'query-node-editor__category_element',
              'btn', 'btn--header-transparent',
              {'query-node-editor__category_element_active': editorState.detailsViewActive})}
            onClick={editorState.onSelectDetailsView}
          >
            {node.label}
          </button>
          {
            props.showTables &&
              <div>
                <div className="query-node-editor__category_header">
                  {T.translate('queryNodeEditor.conceptNodeTables')}
                </div>
                { node.tables.map((table, tableIdx) => (
                    <button
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
                      onClick={() => editorState.onSelectInputTableView(tableIdx)}
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
                      {table.label}
                    </button>
                  ))
                }
              </div>
          }
        </div>

        {
          editorState.detailsViewActive &&
          <div className="query-node-editor__large_column query-node-editor__column">
            <h4>Details</h4>
            <div className="query-node-editor__column_content">
              <div className="query-node-editor__row">
                <label className={classnames('input')}>
                  <span className={classnames("input-label")}>
                    {"Name"}
                  </span>
                  <EditableText
                    loading={false}
                    text={node.label}
                    selectTextOnMount={true}
                    editing={editorState.editingLabel}
                    onSubmit={(value) => { props.onUpdateLabel(value); editorState.onToggleEditLabel(); }}
                    onToggleEdit={editorState.onToggleEditLabel}
                  />
                </label>
              </div>
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
                  <span className={classnames("input-label")}>
                    {"Konzeptbaum"}
                  </span>
                  <ScrollableList items={[node.tree.label]} />
                </label>
              </div>
              <div className="query-node-editor__row">
                <label className={classnames('input')}>
                  <span className={classnames("input-label")}>
                    {"Konzeptcodes"}
                  </span>
                  <ScrollableList items={node.concepts.map(x => x.description ? `${x.label - x.description}` : x.label)} />
                </label>
              </div>
            </div>
          </div>
        }
        {
          !editorState.detailsViewActive && selectedTable != null &&
          <div className="query-node-editor__large_column query-node-editor__column">
            <h4>Filter</h4>
            <div className="query-node-editor__column_content">
              <ParameterTableFilters
                key={editorState.selectedInputTableIdx}
                filters={selectedTable.filters}
                onSetFilterValue={(filterIdx, value) => props.onSetFilterValue(
                  editorState.selectedInputTableIdx,
                  filterIdx,
                  value
                )}
                onSwitchFilterMode={(filterIdx, mode) => props.onSwitchFilterMode(
                  editorState.selectedInputTableIdx,
                  filterIdx,
                  mode
                )}
                onLoadFilterSuggestions={(filterIdx, filterId, prefix) =>
                  props.onLoadFilterSuggestions(
                    props.datasetId,
                    editorState.selectedInputTableIdx,
                    selectedTable.id,
                    node.id,
                    filterIdx,
                    filterId,
                    prefix
                )}
                suggestions={props.suggestions && props.suggestions[editorState.selectedInputTableIdx]}
                onShowDescription={editorState.onShowDescription}
              />
            </div>
          </div>
        }
        {
          !editorState.detailsViewActive &&
          <div className="query-node-editor__fixed_column query-node-editor__column">
            <h4>Description</h4>
            <div className="query-node-editor__column_content">
              <div className="query-node-editor__description">
                { selectedTable != null &&
                  editorState.selectedInput != null &&
                  !isEmpty(selectedTable.filters[editorState.selectedInput].description) &&

                  <span>{selectedTable.filters[editorState.selectedInput].description}</span>
                }
                { selectedTable != null &&
                  editorState.selectedInput != null &&
                  isEmpty(selectedTable.filters[editorState.selectedInput].description) &&
                  <span>No description provided.</span>
                }
                { editorState.selectedInput == null &&
                  <span>Select a filter to see its description here.</span>
                }
              </div>
            </div>
          </div>
        }
      </div>
    </Modal>
  );
};

export const createConnectedQueryNodeEditor = (
  type: string,
  mapStateToProps: Function,
  mapDispatchToProps: Function,
  mergeProps: Function
) => {
  const {
    setDetailsViewActive,
    toggleEditLabel,
    setInputTableViewActive,
    setFocusedInput,
  } = createQueryNodeEditorActions(type);

  const mapDispatchToPropsInternal = (dispatch: Dispatch, ownProps) => {
    const externalDispatchProps = (mapDispatchToProps ? mapDispatchToProps(dispatch, ownProps) : {});

    return {
      ...externalDispatchProps,
      editorState: {
        ...(externalDispatchProps.editorState || {}),
        onSelectDetailsView: () => dispatch(setDetailsViewActive()),
        onToggleEditLabel: () => dispatch(toggleEditLabel()),
        onSelectInputTableView: (tableIdx) => dispatch(setInputTableViewActive(tableIdx)),
        onShowDescription: (filterIdx) => dispatch(setFocusedInput(filterIdx)),
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
      }
    };
  };

  return connect(mapStateToProps, mapDispatchToPropsInternal, mergePropsInternal)(QueryNodeEditor);
};
