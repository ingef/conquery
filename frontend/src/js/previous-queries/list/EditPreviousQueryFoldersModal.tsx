import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import EditableTagsForm from "../../form-components/EditableTagsForm";
import Modal from "../../modal/Modal";

import { useRetagPreviousQuery } from "./actions";
import type { PreviousQueryT } from "./reducer";

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
  const retagPreviousQuery = useRetagPreviousQuery();
  const availableTags = useSelector<StateT, string[]>(
    (state) => state.previousQueries.tags,
  );

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
            await retagPreviousQuery(previousQuery.id, tags);
            onEditSuccess();
          } catch (e) {}
        }}
        onCancel={onClose}
        availableTags={availableTags}
      />
    </Modal>
  );
};

export default EditPreviousQueryFoldersModal;
