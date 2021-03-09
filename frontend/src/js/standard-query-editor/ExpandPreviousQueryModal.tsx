import React, { FC } from "react";
import T from "i18n-react";
import styled from "@emotion/styled";

import Modal from "../modal/Modal";
import PrimaryButton from "../button/PrimaryButton";
import TransparentButton from "../button/TransparentButton";

const Description = styled.p`
  max-width: 400px;
  margin: 0 0 20px;
`;

const Buttons = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-around;
`;

interface Props {
  className?: string;
  onClose: () => void;
  onAccept: () => void;
}

const ExpandPreviousQueryModal: FC<Props> = ({
  onClose,
  onAccept,
  className,
}) => {
  return (
    <Modal
      className={className}
      onClose={onClose}
      headline={T.translate("expandPreviousQueryModal.headline")}
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          onAccept();
        }}
      >
        <Description>
          {T.translate("expandPreviousQueryModal.description")}
        </Description>
        <Buttons>
          <TransparentButton onClick={onClose} type="button">
            {T.translate("common.cancel")}
          </TransparentButton>
          <PrimaryButton autoFocus onClick={onAccept} type="submit">
            {T.translate("expandPreviousQueryModal.submit")}
          </PrimaryButton>
        </Buttons>
      </form>
    </Modal>
  );
};
export default ExpandPreviousQueryModal;
