import React, { FC } from "react";
import { useTranslation } from "react-i18next";

import DeleteModal from "../../modal/DeleteModal";

import { useRemoveQuery } from "./actions";

interface PropsT {
  previousQueryId: string;
  onClose: () => void;
  onDeleteSuccess: () => void;
}

const DeletePreviousQueryModal: FC<PropsT> = ({
  previousQueryId,
  onClose,
  onDeleteSuccess,
}) => {
  const { t } = useTranslation();
  const removeQuery = useRemoveQuery(previousQueryId, onDeleteSuccess);

  return (
    <DeleteModal
      onClose={onClose}
      headline={t("deletePreviousQueryModal.areYouSure")}
      onDelete={removeQuery}
    />
  );
};

export default DeletePreviousQueryModal;
