import React from "react";
import { useTranslation } from "react-i18next";

import DeleteModal from "../../modal/DeleteModal";
import { useDeleteFormConfig } from "../../api/api";
import { useDispatch, useSelector } from "react-redux";
import { StateT } from "app-types";
import type { DatasetIdT } from "../../api/types";
import { setMessage } from "../../snack-message/actions";

interface PropsType {
  formConfigId: string;
  onClose: () => void;
  onDeleteSuccess: () => void;
}

const DeleteFormConfigModal = ({
  formConfigId,
  onClose,
  onDeleteSuccess,
}: PropsType) => {
  const { t } = useTranslation();
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId
  );
  const dispatch = useDispatch();
  const deleteFormConfig = useDeleteFormConfig();

  async function onDeleteFormConfig() {
    if (!datasetId) return;

    try {
      await deleteFormConfig(datasetId, formConfigId);

      onDeleteSuccess();
    } catch (e) {
      dispatch(setMessage(t("formConfig.deleteError")));
    }
  }

  return (
    <DeleteModal
      onClose={onClose}
      headline={t("deleteFormConfigModal.areYouSure")}
      onDelete={onDeleteFormConfig}
    />
  );
};

export default DeleteFormConfigModal;
