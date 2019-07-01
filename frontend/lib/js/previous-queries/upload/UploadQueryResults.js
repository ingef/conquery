// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import type { Dispatch } from "redux-thunk";
import { connect } from "react-redux";

import type { DatasetIdT } from "../../api/types";
import IconButton from "../../button/IconButton";
import actions from "../../app/actions";

import UploadQueryResultsModal from "./UploadQueryResultsModal";
import { openUploadModal, closeUploadModal } from "./actions";

const { startExternalQuery } = actions;

type PropsType = {
  datasetId: ?DatasetIdT,
  isModalOpen: boolean,
  loading: boolean,
  success: ?Object,
  error: ?Object,
  onOpenModal: Function,
  onCloseModal: Function,
  onUpload: Function
};

const Root = styled("div")`
  margin-bottom: 5px;
  padding: 0 10px;
`;

const UploadQueryResults = (props: PropsType) => {
  return (
    <Root>
      <IconButton frame icon="upload" onClick={props.onOpenModal}>
        {T.translate("uploadQueryResults.uploadResults")}
      </IconButton>
      {props.isModalOpen && (
        <UploadQueryResultsModal
          onClose={props.onCloseModal}
          onUpload={query => props.onUpload(props.datasetId, query)}
          loading={props.loading}
          success={props.success}
          error={props.error}
        />
      )}
    </Root>
  );
};

const mapStateToProps = state => ({
  isModalOpen: state.uploadQueryResults.isModalOpen,
  loading:
    state.uploadQueryResults.queryRunner.startQuery.loading ||
    !!state.uploadQueryResults.queryRunner.runningQuery,
  success: state.uploadQueryResults.queryRunner.success,
  error: state.uploadQueryResults.queryRunner.error
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onOpenModal: () => dispatch(openUploadModal()),
  onCloseModal: () => dispatch(closeUploadModal()),
  onUpload: (datasetId, query) => dispatch(startExternalQuery(datasetId, query))
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(UploadQueryResults);
