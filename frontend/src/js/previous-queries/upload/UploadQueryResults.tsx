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

const { startExternalQuery, queryExternalResultReset } = actions;

type PropsType = {
  datasetId: DatasetIdT | null,
  isModalOpen: boolean,
  loading: boolean,
  success: Object | null,
  error: Object | null,
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

const selectQueryRunner = state => state.uploadQueryResults.queryRunner;

const mapStateToProps = state => {
  const queryRunner = selectQueryRunner(state);

  return {
    isModalOpen: state.uploadQueryResults.isModalOpen,
    loading: queryRunner.startQuery.loading || !!queryRunner.runningQuery,
    success: queryRunner.queryResult && queryRunner.queryResult.success,
    error:
      queryRunner.startQuery.error ||
      (!!queryRunner.queryResult && queryRunner.queryResult.error)
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onOpenModal: () => dispatch(openUploadModal()),
  onCloseModal: () =>
    dispatch([closeUploadModal(), queryExternalResultReset()]),
  onUpload: (datasetId, query) => dispatch(startExternalQuery(datasetId, query))
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(UploadQueryResults);
