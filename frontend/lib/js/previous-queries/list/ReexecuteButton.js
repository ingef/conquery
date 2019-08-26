// @flow

import * as React from "react";
import { connect } from "react-redux";

import IconButton from "../../button/IconButton";

import { reexecuteQuery } from "./actions";

import type { DatasetIdT, QueryIdT } from "../../api/types";

type PropsT = {
  previousQueryId: QueryIdT,
  children: React.Node,
  datasetId: DatasetIdT,
  onClick: (datasetId: DatasetIdT, previousQueryId: QueryIdT) => void
};

export default connect(
  state => ({
    datasetId: state.datasets.selectedDatasetId
  }),
  dispatch => ({
    onClick: (...params) => dispatch(reexecuteQuery(...params))
  })
)(({ datasetId, previousQueryId, children, onClick }: PropsT) => {
  return (
    <IconButton
      tight
      bare
      icon="undo"
      onClick={() => onClick(datasetId, previousQueryId)}
    >
      {children}
    </IconButton>
  );
});
