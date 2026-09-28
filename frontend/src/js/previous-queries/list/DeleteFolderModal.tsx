import { useTranslation } from "react-i18next";

import { DeleteModal } from "../../ui-components/DeleteModal";

const DeleteFolderModal = ({
  folder,
  onDeleteFolder,
  onDeleteSuccess,
}: {
  folder: string;
  onDeleteFolder: (folder: string) => Promise<unknown>;
  onDeleteSuccess: () => void;
}) => {
  const { t } = useTranslation();

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
