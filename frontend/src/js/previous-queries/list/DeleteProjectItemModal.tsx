import { FC } from "react";
import { useTranslation } from "react-i18next";

import DeleteModal from "../../modal/DeleteModal";

import type { ProjectItemT } from "./ProjectItem";
import { useRemoveFormConfig, useRemoveQuery } from "./actions";
import { isFormConfig } from "./helpers";

interface PropsT {
  item: ProjectItemT;
  onClose: () => void;
}

const DeleteProjectItemModal: FC<PropsT> = ({ item, onClose }) => {
  const { t } = useTranslation();
  const { removeQuery } = useRemoveQuery();
  const { removeFormConfig } = useRemoveFormConfig();
  const props = isFormConfig(item)
    ? {
        headline: t("deleteFormConfigModal.areYouSure"),
        onDelete: async () => {
          await removeFormConfig(item.id);
          onClose();
        },
      }
    : {
        headline: t("deletePreviousQueryModal.areYouSure"),
        onDelete: async () => {
          await removeQuery(item.id);
          onClose();
        },
      };

  return <DeleteModal onClose={onClose} {...props} />;
};

export default DeleteProjectItemModal;
