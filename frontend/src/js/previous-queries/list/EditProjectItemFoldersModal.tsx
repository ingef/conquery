import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import Modal from "../../modal/Modal";
import EditableTagsForm from "../../ui-components/EditableTagsForm";

import { ProjectItemT } from "./ProjectItem";
import { useRetagQuery, useUpdateFormConfig } from "./actions";
import { isFormConfig } from "./helpers";
import { useFolders } from "./selector";

const SxEditableTagsForm = styled(EditableTagsForm)`
  min-width: 300px;
  max-width: 500px;
`;

interface PropsT {
  item: ProjectItemT;
  onClose: () => void;
  onEditSuccess: () => void;
}

const EditProjectItemFoldersModal: FC<PropsT> = ({
  item,
  onClose,
  onEditSuccess,
}) => {
  const { t } = useTranslation();
  const retagQuery = useRetagQuery();
  const folders = useFolders();
  const { loading: formConfigLoading, updateFormConfig } =
    useUpdateFormConfig();

  const loading = isFormConfig(item) ? formConfigLoading : item.loading;

  return (
    <Modal
      onClose={onClose}
      headline={t("editPreviousQueryFoldersModal.headline")}
    >
      <SxEditableTagsForm
        tags={item.tags}
        loading={loading}
        onSubmit={async (tags) => {
          if (isFormConfig(item)) {
            await updateFormConfig(
              item.id,
              { tags },
              t("formConfig.retagError"),
            );
          } else {
            await retagQuery(item.id, tags);
          }
          onEditSuccess();
        }}
        availableTags={folders}
      />
    </Modal>
  );
};

export default EditProjectItemFoldersModal;
