import * as React from "react";
import { connect } from "react-redux";
import styled from "@emotion/styled";
import T from "i18n-react";

import IconButton from "../../button/IconButton";

import { reexecuteQuery } from "./actions";

export default connect(
  state => ({
    datasetId: state.datasets.selectedDatasetId
  }),
  dispatch => ({
    onClick: (...params) => dispatch(reexecuteQuery(...params))
  })
)(({ datasetId, previousQueryId, children, onClick }) => {
  return (
    <IconButton
      tight
      bare
      icon="undo"
      onClick={() => onClick(datasetId, previousQueryId)}
    >
      {T.translate("previousQuery.reexecute")} {children}
    </IconButton>
  );
});
