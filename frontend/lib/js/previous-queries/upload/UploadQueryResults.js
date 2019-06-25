// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import type { Dispatch } from "redux-thunk";
import { connect } from "react-redux";

import { type DatasetIdType } from "../../dataset/reducer";

import IconButton from "../../button/IconButton";

import UploadQueryResultsModal from "./UploadQueryResultsModal";
import { openUploadModal, closeUploadModal, uploadFile } from "./actions";

type PropsType = {
  datasetId: ?DatasetIdType,
  isModalOpen: boolean,
  loading: boolean,
  success: ?Object,
  error: ?Object,
  onOpenModal: Function,
  onCloseModal: Function,
  onUploadFile: Function
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
          onUploadFile={file => props.onUploadFile(props.datasetId, file)}
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
  loading: state.uploadQueryResults.loading,
  success: state.uploadQueryResults.success,
  error: state.uploadQueryResults.error
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onOpenModal: () => dispatch(openUploadModal()),
  onCloseModal: () => dispatch(closeUploadModal()),
  onUploadFile: (datasetId, file) => dispatch(uploadFile(datasetId, file))
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(UploadQueryResults);
