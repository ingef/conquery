import React from "react";
import T from "i18n-react";

import DeleteModal from "../../modal/DeleteModal";
import { useDeletePreviousQuery } from "./useDeletePreviousQuery";

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
  const onDeletePreviousQuery = useDeletePreviousQuery(
    previousQueryId,
    onDeleteSuccess
  );

  return (
    <DeleteModal
      onClose={onClose}
      headline={T.translate("deletePreviousQueryModal.areYouSure")}
      onDelete={onDeletePreviousQuery}
    />
  );
};

export default DeletePreviousQueryModal;
