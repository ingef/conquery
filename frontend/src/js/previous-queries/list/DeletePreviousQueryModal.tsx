import React, { FC } from "react";
import { useTranslation } from "react-i18next";

import DeleteModal from "../../modal/DeleteModal";

import { useDeletePreviousQuery } from "./useDeletePreviousQuery";

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
  const onDeletePreviousQuery = useDeletePreviousQuery(
    previousQueryId,
    onDeleteSuccess,
  );

  return (
    <DeleteModal
      onClose={onClose}
      headline={t("deletePreviousQueryModal.areYouSure")}
      onDelete={onDeletePreviousQuery}
    />
  );
};

export default DeletePreviousQueryModal;
