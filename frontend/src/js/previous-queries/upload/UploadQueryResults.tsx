import styled from "@emotion/styled";
import { StateT } from "app-types";
import React from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { DatasetIdT } from "../../api/types";
import IconButton from "../../button/IconButton";
import { queryResultReset, useStartQuery } from "../../query-runner/actions";
import { QueryRunnerStateT } from "../../query-runner/reducer";

import UploadQueryResultsModal from "./UploadQueryResultsModal";
import { openUploadModal, closeUploadModal } from "./actions";

interface PropsT {
  datasetId: DatasetIdT | null;
}

const Root = styled("div")`
  margin-bottom: 5px;
  padding: 0 10px;
`;

const UploadQueryResults = ({ datasetId }: PropsT) => {
  const { t } = useTranslation();
  const queryRunner = useSelector<StateT, QueryRunnerStateT>(
    (state) => state.uploadQueryResults.queryRunner,
  );
  const isModalOpen = useSelector<StateT, boolean>(
    (state) => state.uploadQueryResults.isModalOpen,
  );

  const loading = queryRunner.startQuery.loading || !!queryRunner.runningQuery;
  const success =
    !!queryRunner.queryResult && !!queryRunner.queryResult.success;
  const error =
    !!queryRunner.startQuery.error ||
    (!!queryRunner.queryResult && !!queryRunner.queryResult.error);

  const dispatch = useDispatch();
  const onOpenModal = () => dispatch(openUploadModal());

  const startExternalQuery = useStartQuery("external");

  const onCloseModal = () => {
    dispatch(closeUploadModal());
    dispatch(queryResultReset("external"));
  };
  const onUpload = (query: any) => {
    if (datasetId) {
      startExternalQuery(datasetId, query);
    }
  };

  return (
    <Root>
      <IconButton frame icon="upload" onClick={onOpenModal}>
        {t("uploadQueryResults.uploadResults")}
      </IconButton>
      {isModalOpen && (
        <UploadQueryResultsModal
          onClose={onCloseModal}
          onUpload={onUpload}
          loading={loading}
          success={success}
          error={error}
        />
      )}
    </Root>
  );
};

export default UploadQueryResults;
