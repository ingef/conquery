import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import Modal from "../../modal/Modal";
import PrimaryButton from "../../button/PrimaryButton";
import TransparentButton from "../../button/TransparentButton";
import { deleteFormConfig } from "../../api/api";
import { useDispatch, useSelector } from "react-redux";
import { StateT } from "app-types";
import type { DatasetIdT } from "js/api/types";
import { setMessage } from "../../snack-message/actions";

const Root = styled("div")`
  text-align: center;
`;

const Btn = styled(TransparentButton)`
  margin: 0 10px;
`;

const PrimaryBtn = styled(PrimaryButton)`
  margin: 0 10px;
`;

type PropsType = {
  formConfigId: string;
  onClose: () => void;
  onDeleteSuccess: () => void;
};

const DeleteFormConfigModal = ({
  formConfigId,
  onClose,
  onDeleteSuccess,
}: PropsType) => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId
  );
  const dispatch = useDispatch();

  async function onDeleteFormConfig() {
    if (!datasetId) return;

    try {
      await deleteFormConfig(datasetId, formConfigId);

      onDeleteSuccess();
    } catch (e) {
      dispatch(setMessage("formConfig.deleteError"));
    }
  }

  return (
    <Modal
      onClose={onClose}
      headline={T.translate("deleteFormConfigModal.areYouSure")}
    >
      <Root>
        <Btn onClick={onClose}>{T.translate("common.cancel")}</Btn>
        <PrimaryBtn onClick={onDeleteFormConfig}>
          {T.translate("common.delete")}
        </PrimaryBtn>
      </Root>
    </Modal>
  );
};

export default DeleteFormConfigModal;
