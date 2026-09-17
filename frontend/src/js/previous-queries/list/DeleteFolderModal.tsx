import { useTranslation } from "react-i18next";

import { DeleteModal } from "../../ui-components/DeleteModal";

import { useDeleteProjectItemFolder } from "./useDeleteProjectItemFolder";

const DeleteFolderModal = ({
  folder,
  onDeleteSuccess,
}: {
  folder: string;
  onDeleteSuccess: () => void;
}) => {
  const { t } = useTranslation();

  const onDeleteFolder = useDeleteProjectItemFolder();

  return (
    <DeleteModal
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
