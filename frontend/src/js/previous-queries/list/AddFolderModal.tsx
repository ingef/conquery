import styled from "@emotion/styled";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import PrimaryButton from "../../button/PrimaryButton";
import { TransparentButton } from "../../button/TransparentButton";
import Modal from "../../modal/Modal";
import InputPlain from "../../ui-components/InputPlain/InputPlain";

const Buttons = styled("div")`
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
`;

const SxPrimaryButton = styled(PrimaryButton)`
  margin-left: 20px;
`;

const Content = styled("div")`
  max-width: 500px;
`;

interface Props {
  onClose: () => void;
  onSubmit: (folderName: string) => void;
  isValidName: (folderName: string) => boolean;
}

const AddFolderModal = ({ onClose, onSubmit, isValidName }: Props) => {
  const { t } = useTranslation();
  const [folderName, setFolderName] = useState<string>("");

  return (
    <Modal onClose={onClose} closeIcon headline={t("addFolderModal.headline")}>
      <Content>
        <p>{t("addFolderModal.description")}</p>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            onSubmit(folderName);
          }}
        >
          <InputPlain
            label={t("addFolderModal.inputLabel")}
            value={folderName}
            inputType="text"
            onChange={(value) => setFolderName(value as string)}
            inputProps={{ autoFocus: true }}
          />
          <Buttons>
            <TransparentButton onClick={onClose}>
              {t("common.cancel")}
            </TransparentButton>
            <SxPrimaryButton type="submit" disabled={!isValidName(folderName)}>
              {t("common.create")}
            </SxPrimaryButton>
          </Buttons>
        </form>
      </Content>
    </Modal>
  );
};

export default AddFolderModal;
