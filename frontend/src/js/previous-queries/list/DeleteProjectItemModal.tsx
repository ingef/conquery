import { FC } from "react";
import { useTranslation } from "react-i18next";

import DeleteModal from "../../modal/DeleteModal";

import type { ProjectItemT } from "./ProjectItem";
import { useRemoveFormConfig, useRemoveQuery } from "./actions";
import { isFormConfig } from "./helpers";

interface PropsT {
  item: ProjectItemT;
  onClose: () => void;
  onDeleteSuccess: () => void;
}

const DeleteProjectItemModal: FC<PropsT> = ({
  item,
  onClose,
  onDeleteSuccess,
}) => {
  const { t } = useTranslation();
  const removeQuery = useRemoveQuery(item.id, onDeleteSuccess);
  const { removeFormConfig } = useRemoveFormConfig();
  const props = isFormConfig(item)
    ? {
        headline: t("deleteFormConfigModal.areYouSure"),
        onDelete: async () => {
          await removeFormConfig(item.id);
          onDeleteSuccess();
        },
      }
    : {
        headline: t("deletePreviousQueryModal.areYouSure"),
        onDelete: removeQuery,
      };

  return <DeleteModal onClose={onClose} {...props} />;
};

export default DeleteProjectItemModal;
