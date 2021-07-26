import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { DatasetIdT } from "../../api/types";
import IconButton from "../../button/IconButton";
import { queryResultReset, useStartQuery } from "../../query-runner/actions";
import { QueryRunnerStateT } from "../../query-runner/reducer";
import WithTooltip from "../../tooltip/WithTooltip";

import UploadQueryResultsModal from "./UploadQueryResultsModal";

const SxIconButton = styled(IconButton)`
  padding: 8px 6px;
`;

interface PropsT {
  className?: string;
  datasetId: DatasetIdT | null;
}

const UploadQueryResults = ({ className, datasetId }: PropsT) => {
  const { t } = useTranslation();
  const queryRunner = useSelector<StateT, QueryRunnerStateT>(
    (state) => state.uploadQueryResults.queryRunner,
  );

  const loading = queryRunner.startQuery.loading || !!queryRunner.runningQuery;
  const success =
    !!queryRunner.queryResult && !!queryRunner.queryResult.success;
  const error =
    !!queryRunner.startQuery.error ||
    (!!queryRunner.queryResult && !!queryRunner.queryResult.error);

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const dispatch = useDispatch();

  const startExternalQuery = useStartQuery("external");

  const onCloseModal = () => {
    setIsModalOpen(false);
    dispatch(queryResultReset("external"));
  };
  const onUpload = (query: any) => {
    if (datasetId) {
      startExternalQuery(datasetId, query);
    }
  };

  return (
    <>
      <WithTooltip
        text={t("uploadQueryResults.uploadResults")}
        className={className}
      >
        <SxIconButton
          frame
          icon="upload"
          onClick={() => setIsModalOpen(true)}
        />
      </WithTooltip>
      {isModalOpen && (
        <UploadQueryResultsModal
          onClose={onCloseModal}
          onUpload={onUpload}
          loading={loading}
          success={success}
          error={error}
        />
      )}
    </>
  );
};

export default UploadQueryResults;
