import { useTranslation } from "react-i18next";

import DeleteModal from "../../modal/DeleteModal";

import { useDeleteProjectItemFolder } from "./useDeleteProjectItemFolder";

const DeleteFolderModal = ({
  folder,
  onClose,
  onDeleteSuccess,
}: {
  folder: string;
  onClose: () => void;
  onDeleteSuccess: () => void;
}) => {
  const { t } = useTranslation();

  const onDeleteFolder = useDeleteProjectItemFolder();

  return (
    <DeleteModal
      onClose={onClose}
      headline={t("deletePreviousQueryFolderModal.areYouSure")}
      description={t("deletePreviousQueryFolderModal.description", { folder })}
      onDelete={async () => {
        await onDeleteFolder(folder);
        onDeleteSuccess();
      }}
    />
  );
};

export default DeleteFolderModal;
