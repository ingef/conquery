import styled from "@emotion/styled";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";

import PrimaryButton from "../button/PrimaryButton";
import TransparentButton from "../button/TransparentButton";
import Modal from "../modal/Modal";

const Description = styled.p`
  max-width: 400px;
  margin: 0 0 20px;
`;

const Buttons = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  const { t } = useTranslation();

  return (
    <Modal
      className={className}
      onClose={onClose}
      headline={t("expandPreviousQueryModal.headline")}
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          onAccept();
        }}
      >
        <Description>{t("expandPreviousQueryModal.description")}</Description>
        <Buttons>
          <TransparentButton onClick={onClose} type="button">
            {t("common.cancel")}
          </TransparentButton>
          <PrimaryButton autoFocus onClick={onAccept} type="submit">
            {t("expandPreviousQueryModal.submit")}
          </PrimaryButton>
        </Buttons>
      </form>
    </Modal>
  );
};
export default ExpandPreviousQueryModal;
