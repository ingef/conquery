import { FC } from "react";
import { useTranslation } from "react-i18next";

import DeleteModal from "../../modal/DeleteModal";

import { useDeletePreviousQueryFolder } from "./useDeletePreviousQueryFolder";

interface PropsT {
  folder: string;
  onClose: () => void;
  onDeleteSuccess: () => void;
}

const DeletePreviousQueryFolderModal: FC<PropsT> = ({
  folder,
  onClose,
  onDeleteSuccess,
}) => {
  const { t } = useTranslation();
  const onDeletePreviousQuery = useDeletePreviousQueryFolder(
    folder,
    onDeleteSuccess,
  );

  return (
    <DeleteModal
      onClose={onClose}
      headline={t("deletePreviousQueryFolderModal.areYouSure")}
      description={t("deletePreviousQueryFolderModal.description", { folder })}
      onDelete={onDeletePreviousQuery}
    />
  );
};

export default DeletePreviousQueryFolderModal;
