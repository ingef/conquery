// @flow

import React from "react";
import T from "i18n-react";
import type { Dispatch } from "redux-thunk";
import { connect } from "react-redux";

import { type DatasetIdType } from "../../dataset/reducer";

import IconButton from "../../button/IconButton";

import UploadQueryResultsModal from "./UploadQueryResultsModal";
import { openUploadModal, closeUploadModal, uploadFile } from "./actions";
import { type UploadReportType } from "./reducer";

type PropsType = {
  datasetId: DatasetIdType,
  isModalOpen: boolean,
  loading: boolean,
  success: ?UploadReportType,
  error: ?(UploadReportType & { message: string }),
  onOpenModal: Function,
  onCloseModal: Function,
  onUploadFile: Function
};

const UploadQueryResults = (props: PropsType) => {
  return (
    <div className="upload-query-results">
      <IconButton frame icon="upload" onClick={props.onOpenModal}>
        {T.translate("uploadQueryResults.uploadResults")}
      </IconButton>
      {props.isModalOpen && (
        <UploadQueryResultsModal
          onCloseModal={props.onCloseModal}
          onUploadFile={file => props.onUploadFile(props.datasetId, file)}
          loading={props.loading}
          success={props.success}
          error={props.error}
        />
      )}
    </div>
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
