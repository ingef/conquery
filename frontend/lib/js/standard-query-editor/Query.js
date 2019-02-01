// @flow

import React                      from 'react';
import { connect }                from 'react-redux';
import type { Dispatch }          from 'redux';
import T                          from 'i18n-react';
import { replace }                from 'react-router-redux';

import { toQuery }                from '../routes'
import {
  queryGroupModalSetNode,
}                                 from '../query-group-modal/actions';
import {
  loadPreviousQuery,
  loadAllPreviousQueriesInGroups,
}                                 from '../previous-queries/list/actions';
import {
  dropFiles,
  dropFilesDateRangeType,
  dropFilesAndIdx,
}                                 from '../file-upload/actions';
import type {
  DraggedFileType,
  GenericFileType
}                                 from '../file-upload/types';
import type { DateRangeType }     from '../common/types/backend';

import {
  dropAndNode,
  dropOrNode,
  deleteNode,
  deleteGroup,
  toggleExcludeGroup,
  expandPreviousQuery,
  selectNodeForEditing,
}                                 from './actions'
import type {
  StandardQueryType,
  DraggedNodeType,
  DraggedQueryType
}                                 from './types';
import { QueryEditorDropzone }    from './QueryEditorDropzone';
import QueryGroup                 from './QueryGroup';


type PropsType = {
  query: StandardQueryType,
  isEmptyQuery: boolean,
  dropAndNode: (DraggedNodeType | DraggedQueryType, ?DateRangeType) => void,
  dropFiles: (DraggedFileType, ?GenericFileType) => void,
  dropFilesDateRangeType: (DraggedFileType, ?DateRangeType) => void,
  dropFilesAndIdx: (DraggedFileType, ?number) => void,
  dropOrNode: (DraggedNodeType | DraggedQueryType, number) => void,
  deleteNode: Function,
  deleteGroup: Function,
  toggleExcludeGroup: Function,
  expandPreviousQuery: Function,
  loadPreviousQuery: Function,
  selectNodeForEditing: Function,
  queryGroupModalSetNode: Function,
  dateRange: Object,
};

const Query = (props: PropsType) => {
  return (
    <div className="query-editor__query-container">
      {
        props.isEmptyQuery &&
        // Render a large Dropzone
        <QueryEditorDropzone
          isInitial
          onDropNode={item => props.dropAndNode(item)}
          onDropFiles={props.dropFiles}
          onLoadPreviousQuery={props.loadPreviousQuery}
        />
      }
      <div className="query-editor__query">
        {
          !props.isEmptyQuery &&
          // Render all query groups plus individual AND / OR dropzones
          props.query.map((group, andIdx) => ([
              <QueryGroup
                key={andIdx}
                group={group}
                andIdx={andIdx}
                onDropNode={item => props.dropOrNode(item, andIdx)}
                onDropFiles={item => props.dropFilesAndIdx(item, andIdx)}
                onDeleteNode={orIdx => props.deleteNode(andIdx, orIdx)}
                onDeleteGroup={orIdx => props.deleteGroup(andIdx, orIdx)}
                onFilterClick={orIdx => props.selectNodeForEditing(andIdx, orIdx)}
                onExpandClick={props.expandPreviousQuery}
                onExcludeClick={() => props.toggleExcludeGroup(andIdx)}
                onDateClick={() => props.queryGroupModalSetNode(andIdx)}
                onLoadPreviousQuery={props.loadPreviousQuery}
              />,
              <p key={`${andIdx}.and`} className="query-group-connector">
                {T.translate('common.and')}
              </p>
            ])).concat(
              <div
                className="dropzone-wrap"
                key={props.query.length + 1}
              >
                <QueryEditorDropzone
                  isAnd
                  onDropNode={item => props.dropAndNode(item, props.dateRange)}
                  onDropFiles={item => props.dropFilesDateRangeType(item, props.dateRange)}
                  onLoadPreviousQuery={props.loadPreviousQuery}
                />
              </div>
            )
        }
      </div>
    </div>
  );
};

function mapStateToProps(state) {
  return {
    query: state.panes.right.tabs.queryEditor.query,
    isEmptyQuery: state.panes.right.tabs.queryEditor.query.length === 0,

    // only used by other actions
    rootConcepts: state.categoryTrees.trees,
  };
}

const mapDispatchToProps = (dispatch: Dispatch<*>) => ({
  dropAndNode: (item, dateRange) => dispatch(dropAndNode(item, dateRange)),
  dropFiles: (item, type) => dispatch(dropFiles(item, type)),
  dropFilesDateRangeType: (item, dateRange) =>
    dispatch(dropFilesDateRangeType(item, dateRange)),
  dropFilesAndIdx: (item, andIdx) => dispatch(dropFilesAndIdx(item, andIdx)),
  dropOrNode: (item, andIdx) => dispatch(dropOrNode(item, andIdx)),
  deleteNode: (andIdx, orIdx) => dispatch(deleteNode(andIdx, orIdx)),
  deleteGroup: (andIdx, orIdx) => dispatch(deleteGroup(andIdx, orIdx)),
  toggleExcludeGroup: (andIdx) => dispatch(toggleExcludeGroup(andIdx)),
  selectNodeForEditing: (andIdx, orIdx) =>
    dispatch(selectNodeForEditing(andIdx, orIdx)),
  queryGroupModalSetNode: (andIdx) =>
    dispatch(queryGroupModalSetNode(andIdx)),
  expandPreviousQuery: (datasetId, rootConcepts, groups, queryId) => {
    dispatch(expandPreviousQuery(rootConcepts, groups));

    dispatch(loadAllPreviousQueriesInGroups(groups, datasetId));

    dispatch(replace(toQuery(datasetId, queryId)));
  },
  loadPreviousQuery: (datasetId, queryId) =>
    dispatch(loadPreviousQuery(datasetId, queryId)),
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  loadPreviousQuery: (queryId) =>
    dispatchProps.loadPreviousQuery(
      ownProps.selectedDatasetId,
      queryId
    ),
  expandPreviousQuery: (groups, queryId) =>
    dispatchProps.expandPreviousQuery(
      ownProps.selectedDatasetId,
      stateProps.rootConcepts,
      groups,
      queryId
    ),
});

export default connect(mapStateToProps, mapDispatchToProps, mergeProps)(Query);
