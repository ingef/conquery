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
  dropAndNode,
  dropConceptListFile,
  dropOrConceptListFile,
  dropOrNode,
  deleteNode,
  deleteGroup,
  toggleExcludeGroup,
  expandPreviousQuery,
  selectNodeForEditing,
}                                 from './actions'
import type { StandardQueryType, DateRangeType, DraggedNodeType, DraggedQueryType, DraggedFileType } from './types';

import { QueryEditorDropzone }    from './QueryEditorDropzone';
import QueryGroup                 from './QueryGroup';


type PropsType = {
  query: StandardQueryType,
  isEmptyQuery: boolean,
  dropAndNode: (DraggedNodeType | DraggedQueryType, ?DateRangeType) => void,
  dropConceptListFile: (DraggedFileType, ?DateRangeType) => void,
  dropOrNode: (DraggedNodeType | DraggedQueryType, number) => void,
  dropOrConceptListFile: (DraggedFileType, number) => void,
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
          onDropNode={item => props.dropAndNode(item, null)}
          onDropFiles={props.dropConceptListFile}
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
                onDropFiles={item => props.dropOrConceptListFile(item, andIdx)}
                onDeleteNode={orIdx => props.deleteNode(andIdx, orIdx)}
                onDeleteGroup={() => props.deleteGroup(andIdx)}
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
                  onDropFiles={item => props.dropConceptListFile(item, props.dateRange)}
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
    query: state.query,
    isEmptyQuery: state.query.length === 0,

    // only used by other actions
    rootConcepts: state.categoryTrees.trees,
  };
}

const mapDispatchToProps = (dispatch: Dispatch<*>) => ({
  dropAndNode: (item, dateRange) => dispatch(dropAndNode(item, dateRange)),
  dropConceptListFile: (item, dateRange) => dispatch(dropConceptListFile(item, { dateRange })),
  dropOrConceptListFile: (item, andIdx) => dispatch(dropOrConceptListFile(item, andIdx)),
  dropOrNode: (item, andIdx) => dispatch(dropOrNode(item, andIdx)),
  deleteNode: (andIdx, orIdx) => dispatch(deleteNode(andIdx, orIdx)),
  deleteGroup: (andIdx) => dispatch(deleteGroup(andIdx)),
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
