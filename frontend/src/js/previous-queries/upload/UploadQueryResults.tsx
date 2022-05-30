import styled from "@emotion/styled";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import { usePostQueryUpload } from "../../api/api";
import type {
  DatasetT,
  QueryUploadConfigT,
  UploadQueryResponseT,
} from "../../api/types";
import type { StateT } from "../../app/reducers";
import IconButton from "../../button/IconButton";
import { setMessage } from "../../snack-message/actions";
import WithTooltip from "../../tooltip/WithTooltip";
import { useLoadQueries } from "../list/actions";

import { QueryToUploadT } from "./CSVColumnPicker";
import UploadQueryResultsModal from "./UploadQueryResultsModal";

const SxIconButton = styled(IconButton)`
  padding: 8px 6px;
`;

interface PropsT {
  className?: string;
  datasetId: DatasetT["id"] | null;
}

const UploadQueryResults = ({ className, datasetId }: PropsT) => {
  const { t } = useTranslation();

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [uploadResult, setUploadResult] = useState<UploadQueryResponseT | null>(
    null,
  );

  const dispatch = useDispatch();
  const postQueryUpload = usePostQueryUpload();
  const { loadQueries } = useLoadQueries();

  const queryUploadConfig = useSelector<StateT, QueryUploadConfigT>(
    (state) => state.startup.config.queryUpload,
  );

  const onCloseModal = () => {
    setIsModalOpen(false);
    setUploadResult(null);
  };
  const onUpload = async (query: QueryToUploadT) => {
    if (!datasetId) return;

    try {
      setUploadResult(null);
      setLoading(true);

      const result = await postQueryUpload(datasetId, query);

      setUploadResult(result);

      loadQueries(datasetId);
    } catch (e) {
      if ((e as { status?: number }).status === 400) {
        setUploadResult(e as UploadQueryResponseT);
      } else {
        dispatch(
          setMessage({ message: t("uploadQueryResultsModal.uploadFailed") }),
        );
      }
    }
    setLoading(false);
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
          loading={loading}
          uploadResult={uploadResult}
          config={queryUploadConfig}
          onClearUploadResult={() => setUploadResult(null)}
          onClose={onCloseModal}
          onUpload={onUpload}
        />
      )}
    </>
  );
};

export default UploadQueryResults;
