import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import Modal from "../../modal/Modal";
import EditableTagsForm from "../../ui-components/EditableTagsForm";

import { ProjectItemT } from "./ProjectItem";
import { useUpdateFormConfig, useUpdateQuery } from "./actions";
import { isFormConfig } from "./helpers";
import { useFolders } from "./selector";

const SxEditableTagsForm = styled(EditableTagsForm)`
  min-width: 300px;
  max-width: 500px;
`;

interface PropsT {
  item: ProjectItemT;
  onClose: () => void;
}

const EditProjectItemFoldersModal: FC<PropsT> = ({ item, onClose }) => {
  const { t } = useTranslation();
  const folders = useFolders();
  const { loading: queryLoading, updateQuery } = useUpdateQuery();
  const { loading: formConfigLoading, updateFormConfig } =
    useUpdateFormConfig();

  const loading = queryLoading || formConfigLoading;

  const onSubmit = async (tags: string[]) => {
    if (isFormConfig(item)) {
      await updateFormConfig(item.id, { tags }, t("formConfig.retagError"));
    } else {
      await updateQuery(item.id, { tags }, t("previousQuery.retagError"));
    }
    onClose();
  };

  return (
    <Modal
      onClose={onClose}
      headline={t("editPreviousQueryFoldersModal.headline")}
    >
      <SxEditableTagsForm
        tags={item.tags}
        loading={loading}
        onSubmit={onSubmit}
        availableTags={folders}
      />
    </Modal>
  );
};

export default EditProjectItemFoldersModal;
