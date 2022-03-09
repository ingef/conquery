import { FC } from "react";
import { useTranslation } from "react-i18next";

import DeleteModal from "../../modal/DeleteModal";

import { useDeleteProjectItemFolder } from "./useDeleteProjectItemFolder";

interface PropsT {
  folder: string;
  onClose: () => void;
  onDeleteSuccess: () => void;
}

const DeleteFolderModal: FC<PropsT> = ({
  folder,
  onClose,
  onDeleteSuccess,
}) => {
  const { t } = useTranslation();
  const onDeleteFolder = useDeleteProjectItemFolder(folder, onDeleteSuccess);

  return (
    <DeleteModal
      onClose={onClose}
      headline={t("deletePreviousQueryFolderModal.areYouSure")}
      description={t("deletePreviousQueryFolderModal.description", { folder })}
      onDelete={onDeleteFolder}
    />
  );
};

export default DeleteFolderModal;
