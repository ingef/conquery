import { faUpload } from "@fortawesome/free-solid-svg-icons";
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
import { Tooltip, TooltipTrigger } from "../../ui-components/Tooltip";
import { useLoadQueries } from "../list/actions";

import type { QueryToUploadT } from "./CSVColumnPicker";
import UploadQueryResultsModal from "./UploadQueryResultsModal";

const UploadQueryResults = ({
  className,
  datasetId,
}: {
  className?: string;
  datasetId: DatasetT["id"] | null;
}) => {
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
      if (
        (e as { status?: number }).status === 400 &&
        "resolved" in (e as object)
      ) {
        setUploadResult(e as UploadQueryResponseT);
      } else {
        dispatch(
          setMessage({
            message: t("uploadQueryResultsModal.uploadFailed"),
            type: "error",
          }),
        );
      }
    }
    setLoading(false);
  };

  return (
    <div className={className}>
      <TooltipTrigger>
        <IconButton
          className="px-[6px] py-[9px]"
          frame
          icon={faUpload}
          onClick={() => setIsModalOpen(true)}
        />
        <Tooltip>{t("uploadQueryResults.uploadResults")}</Tooltip>
      </TooltipTrigger>
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
    </div>
  );
};

export default UploadQueryResults;
