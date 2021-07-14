import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import EditableTagsForm from "../../form-components/EditableTagsForm";
import Modal from "../../modal/Modal";

import { useRetagQuery } from "./actions";
import type { PreviousQueryT } from "./reducer";
import { usePreviousQueriesTags } from "./selector";

const SxEditableTagsForm = styled(EditableTagsForm)`
  min-width: 300px;
  max-width: 500px;
`;

interface PropsT {
  previousQuery: PreviousQueryT;
  onClose: () => void;
  onEditSuccess: () => void;
}

const EditPreviousQueryFoldersModal: FC<PropsT> = ({
  previousQuery,
  onClose,
  onEditSuccess,
}) => {
  const { t } = useTranslation();
  const retagQuery = useRetagQuery();
  const folders = usePreviousQueriesTags();

  return (
    <Modal
      onClose={onClose}
      headline={t("editPreviousQueryFoldersModal.headline")}
    >
      <SxEditableTagsForm
        tags={previousQuery.tags}
        loading={previousQuery.loading}
        onSubmit={async (tags) => {
          try {
            await retagQuery(previousQuery.id, tags);
            onEditSuccess();
          } catch (e) {}
        }}
        onCancel={onClose}
        availableTags={folders}
      />
    </Modal>
  );
};

export default EditPreviousQueryFoldersModal;
