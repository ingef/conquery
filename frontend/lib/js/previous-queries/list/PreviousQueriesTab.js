// @flow

import React from "react";
import styled from "@emotion/styled";
import { connect } from "react-redux";

import DeletePreviousQueryModal from "../delete-modal/DeletePreviousQueryModal";
import PreviousQueriesSearchBox from "../search/PreviousQueriesSearchBox";
import PreviousQueriesFilter from "../filter/PreviousQueriesFilter";
import PreviousQueries from "./PreviousQueries";
import UploadQueryResults from "../upload/UploadQueryResults";

import EmptyList from "./EmptyList";
import PreviousQueriesLoading from "./PreviousQueriesLoading";

import { loadPreviousQueries } from "./actions";
import { selectPreviousQueries } from "./selector";

const Container = styled("div")`
  overflow-y: auto;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 0 10px 0 20px;
`;

class PreviousQueryEditorTab extends React.Component {
  componentDidMount() {
    this.props.loadQueries();
  }

  render() {
    const { datasetId, queries, loading } = this.props;

    const hasQueries = loading || queries.length !== 0;

    return (
      <>
        <PreviousQueriesFilter />
        <PreviousQueriesSearchBox isMulti />
        <UploadQueryResults datasetId={datasetId} />
        <Container>
          {loading && <PreviousQueriesLoading />}
          {this.props.queries.length === 0 && !loading && <EmptyList />}
        </Container>
        {hasQueries && (
          <>
            <PreviousQueries queries={queries} datasetId={datasetId} />
            <DeletePreviousQueryModal datasetId={datasetId} />
          </>
        )}
      </>
    );
  }
}

export default connect(
  state => ({
    queries: selectPreviousQueries(
      state.previousQueries.queries,
      state.previousQueriesSearch,
      state.previousQueriesFilter
    ),
    loading: state.previousQueries.loading
  }),
  (dispatch, ownProps) => ({
    loadQueries: () => dispatch(loadPreviousQueries(ownProps.datasetId))
  })
)(PreviousQueryEditorTab);
