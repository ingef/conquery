import React, { ReactNode } from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import Modal from "./Modal";
import PrimaryButton from "../button/PrimaryButton";
import TransparentButton from "../button/TransparentButton";

const Root = styled("div")`
  text-align: center;
`;

const Btn = styled(TransparentButton)`
  margin: 0 10px;
`;

const PrimaryBtn = styled(PrimaryButton)`
  margin: 0 10px;
`;

interface PropsType {
  headline: ReactNode;
  onClose: () => void;
  onDelete: () => void;
}

const DeleteModal = ({ headline, onClose, onDelete }: PropsType) => {
  return (
    <Modal onClose={onClose} headline={headline}>
      <Root>
        <Btn onClick={onClose}>{T.translate("common.cancel")}</Btn>
        <PrimaryBtn onClick={onDelete}>
          {T.translate("common.delete")}
        </PrimaryBtn>
      </Root>
    </Modal>
  );
};

export default DeleteModal;
