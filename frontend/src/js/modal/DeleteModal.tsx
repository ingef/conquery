import styled from "@emotion/styled";
import { ReactNode } from "react";
import { useTranslation } from "react-i18next";

import PrimaryButton from "../button/PrimaryButton";
import { TransparentButton } from "../button/TransparentButton";

import Modal from "./Modal";

const Root = styled("div")`
  text-align: center;
`;

const Description = styled("p")`
  margin: 0 0 20px;
`;

const Btn = styled(TransparentButton)`
  margin: 0 10px;
`;

const PrimaryBtn = styled(PrimaryButton)`
  margin: 0 10px;
`;

interface PropsType {
  headline: ReactNode;
  description?: ReactNode;
  onClose: () => void;
  onDelete: () => void;
}

const DeleteModal = ({
  headline,
  description,
  onClose,
  onDelete,
}: PropsType) => {
  const { t } = useTranslation();

  return (
    <Modal onClose={onClose} headline={headline}>
      {description && <Description>{description}</Description>}
      <Root>
        <Btn onClick={onClose}>{t("common.cancel")}</Btn>
        <PrimaryBtn onClick={onDelete}>{t("common.delete")}</PrimaryBtn>
      </Root>
    </Modal>
  );
};

export default DeleteModal;
