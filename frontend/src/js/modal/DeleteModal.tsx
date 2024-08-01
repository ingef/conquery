import styled from "@emotion/styled";
import { ReactNode } from "react";
import { useTranslation } from "react-i18next";

import { DestroyButton } from "../button/DestroyButton";
import { TransparentButton } from "../button/TransparentButton";

import Modal from "./Modal";

const Root = styled("div")`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
`;

const Content = styled("div")`
  max-width: 400px;
`;

const Description = styled("p")`
  margin: 0 0 20px;
`;

const Btn = styled(TransparentButton)`
  margin: 0 10px;
`;

interface PropsType {
  headline: ReactNode;
  description?: ReactNode;
  onClose: () => void;
  onDelete: () => Promise<unknown>;
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
      <Content>
        {description && <Description>{description}</Description>}
        <Root>
          <Btn onClick={onClose}>{t("common.cancel")}</Btn>
          <DestroyButton onClick={onDelete}>{t("common.delete")}</DestroyButton>
        </Root>
      </Content>
    </Modal>
  );
};

export default DeleteModal;
