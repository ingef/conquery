import React from "react";
import T from "i18n-react";

import DeleteModal from "../../modal/DeleteModal";
import { deleteStoredQuery } from "../../api/api";
import { useDispatch, useSelector } from "react-redux";
import { StateT } from "app-types";
import type { DatasetIdT } from "js/api/types";
import { setMessage } from "../../snack-message/actions";

interface PropsType {
  previousQueryId: string;
  onClose: () => void;
  onDeleteSuccess: () => void;
}

const DeletePreviousQueryModal = ({
  previousQueryId,
  onClose,
  onDeleteSuccess,
}: PropsType) => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId
  );
  const dispatch = useDispatch();

  async function onDeletePreviousQuery() {
    if (!datasetId) return;

    try {
      await deleteStoredQuery(datasetId, previousQueryId);

      onDeleteSuccess();
    } catch (e) {
      dispatch(setMessage("previousQuery.deleteError"));
    }
  }

  return (
    <DeleteModal
      onClose={onClose}
      headline={T.translate("deletePreviousQueryModal.areYouSure")}
      onDelete={onDeletePreviousQuery}
    />
  );
};

export default DeletePreviousQueryModal;
