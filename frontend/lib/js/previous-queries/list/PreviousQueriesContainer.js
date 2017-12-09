import React, { PropTypes }      from 'react';
import { connect }               from 'react-redux';

import { loadPreviousQueries }   from './actions';
import { selectPreviousQueries } from './selector';
import PreviousQueries           from './PreviousQueries';

const PreviousQueriesContainer = (props) => {
  return (
    <PreviousQueries
      datasetId={props.datasetId}
      queries={props.queries}
      loading={props.loading}
      error={props.error}
      loadQueries={props.loadQueries}
    />
  );
};

PreviousQueriesContainer.propTypes = {
  datasetId: PropTypes.string.isRequired,
  queries: PropTypes.array.isRequired,
  loading: PropTypes.bool,
  error: PropTypes.string,
  loadQueries: PropTypes.func.isRequired,
};

const mapStateToProps = (state) => ({
  queries: selectPreviousQueries(
    state.previousQueries.queries,
    state.previousQueriesSearch,
    state.previousQueriesFilter,
  ),
  loading: state.previousQueries.loading,
  error: state.previousQueries.error,
});

const mapDispatchToProps = (dispatch) => ({
  loadQueries: (datasetId) => dispatch(loadPreviousQueries(datasetId)),
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...ownProps,
  ...stateProps,
  ...dispatchProps,
  loadQueries: () => dispatchProps.loadQueries(ownProps.datasetId)
});

export default connect(mapStateToProps, mapDispatchToProps, mergeProps)(PreviousQueriesContainer);
