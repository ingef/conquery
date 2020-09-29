import React from "react";
import styled from "@emotion/styled";

import { connect } from "react-redux";
import T from "i18n-react";

import type { DateRangeT } from "../api/types";

import { queryGroupModalSetNode } from "../query-group-modal/actions";
import { loadPreviousQuery } from "../previous-queries/list/actions";
import { openQueryUploadConceptListModal } from "../query-upload-concept-list-modal/actions";

import {
  dropAndNode,
  dropOrNode,
  deleteNode,
  deleteGroup,
  toggleExcludeGroup,
  expandPreviousQuery,
  selectNodeForEditing,
  toggleTimestamps
} from "./actions";

import type {
  StandardQueryType,
  DraggedNodeType,
  DraggedQueryType
} from "./types";
import QueryEditorDropzone from "./QueryEditorDropzone";
import QueryGroup from "./QueryGroup";

type PropsT = {
  query: StandardQueryType;
  isEmptyQuery: boolean;
  dropAndNode: (
    node: DraggedNodeType | DraggedQueryType,
    range: DateRangeT | null
  ) => void;
  dropOrNode: (node: DraggedNodeType | DraggedQueryType, idx: number) => void;
  deleteNode: Function;
  deleteGroup: Function;
  dropConceptListFile: Function;
  toggleExcludeGroup: Function;
  expandPreviousQuery: Function;
  loadPreviousQuery: Function;
  selectNodeForEditing: Function;
  queryGroupModalSetNode: Function;
  toggleTimestamps: Function;
  dateRange: Object;
};

const Container = styled("div")`
  height: 100%;
`;

const Groups = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  padding: 0 0 20px;
`;

const QueryGroupConnector = styled("p")`
  padding: 110px 6px 0;
  margin: 0;
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme }) => theme.col.gray};
  text-align: center;
`;

const Query = (props: PropsT) => {
  return (
    <Container>
      {props.isEmptyQuery ? (
        <QueryEditorDropzone
          isInitial
          onDropNode={props.dropAndNode}
          onDropFile={props.dropConceptListFile}
          onLoadPreviousQuery={props.loadPreviousQuery}
        />
      ) : (
        <Groups>
          {props.query
            .map((group, andIdx) => [
              <QueryGroup
                key={andIdx}
                group={group}
                andIdx={andIdx}
                onDropNode={item => props.dropOrNode(item, andIdx)}
                onDropFile={file => props.dropConceptListFile(file, andIdx)}
                onDeleteNode={orIdx => props.deleteNode(andIdx, orIdx)}
                onDeleteGroup={orIdx => props.deleteGroup(andIdx, orIdx)}
                onEditClick={orIdx => props.selectNodeForEditing(andIdx, orIdx)}
                onExpandClick={props.expandPreviousQuery}
                onExcludeClick={() => props.toggleExcludeGroup(andIdx)}
                onDateClick={() => props.queryGroupModalSetNode(andIdx)}
                onLoadPreviousQuery={props.loadPreviousQuery}
                onToggleTimestamps={orIdx =>
                  props.toggleTimestamps(andIdx, orIdx)
                }
              />,
              <QueryGroupConnector key={`${andIdx}.and`}>
                {T.translate("common.and")}
              </QueryGroupConnector>
            ])
            .concat(
              <QueryEditorDropzone
                key={props.query.length + 1}
                isAnd
                onDropNode={item => props.dropAndNode(item, props.dateRange)}
                onDropFile={props.dropConceptListFile}
                onLoadPreviousQuery={props.loadPreviousQuery}
              />
            )}
        </Groups>
      )}
    </Container>
  );
};

function mapStateToProps(state) {
  return {
    query: state.queryEditor.query,
    isEmptyQuery: state.queryEditor.query.length === 0,

    // only used by other actions
    rootConcepts: state.conceptTrees.trees
  };
}

const mapDispatchToProps = dispatch => ({
  dropAndNode: (item, dateRange) => dispatch(dropAndNode(item, dateRange)),
  dropConceptListFile: (file, andIdx) =>
    dispatch(openQueryUploadConceptListModal(andIdx, file)),
  dropOrNode: (item, andIdx) => dispatch(dropOrNode(item, andIdx)),
  deleteNode: (andIdx, orIdx) => dispatch(deleteNode(andIdx, orIdx)),
  deleteGroup: (andIdx, orIdx) => dispatch(deleteGroup(andIdx, orIdx)),
  toggleExcludeGroup: andIdx => dispatch(toggleExcludeGroup(andIdx)),
  toggleTimestamps: (andIdx, orIdx) =>
    dispatch(toggleTimestamps(andIdx, orIdx)),
  selectNodeForEditing: (andIdx, orIdx) =>
    dispatch(selectNodeForEditing(andIdx, orIdx)),
  queryGroupModalSetNode: andIdx => dispatch(queryGroupModalSetNode(andIdx)),
  expandPreviousQuery: (datasetId, rootConcepts, queryId) =>
    dispatch(expandPreviousQuery(datasetId, rootConcepts, queryId)),
  loadPreviousQuery: (datasetId, queryId) =>
    dispatch(loadPreviousQuery(datasetId, queryId))
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  loadPreviousQuery: queryId =>
    dispatchProps.loadPreviousQuery(ownProps.selectedDatasetId, queryId),
  expandPreviousQuery: queryId =>
    dispatchProps.expandPreviousQuery(
      ownProps.selectedDatasetId,
      stateProps.rootConcepts,
      queryId
    )
});

export default connect(mapStateToProps, mapDispatchToProps, mergeProps)(Query);
