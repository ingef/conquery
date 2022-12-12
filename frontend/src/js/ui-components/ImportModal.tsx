import styled from "@emotion/styled";
import { MouseEvent, useState } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import PrimaryButton from "../button/PrimaryButton";
import Modal from "../modal/Modal";

const Content = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 20px;
`;

const Row = styled("div")`
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
`;

const Textarea = styled("textarea")`
  width: 100%;
`;

export const ImportModal = ({
  onClose,
  onSubmit,
}: {
  onClose: () => void;
  onSubmit: (lines: string[]) => void;
}) => {
  const { t } = useTranslation();
  const [textInput, setTextInput] = useState("");

  const onPasteClick = async () => {
    if (navigator.clipboard) {
      const text = await navigator.clipboard.readText();
      const lines = text.split("\n").map((line) => line.trim());

      setTextInput(lines.join("\n"));
    }
  };

  const onSubmitClick = (
    e: MouseEvent<HTMLButtonElement, globalThis.MouseEvent>,
  ) => {
    e.stopPropagation();

    const lines = textInput.split("\n").map((line) => line.trim());

    onSubmit(lines);
    onClose();
  };

  return (
    <Modal
      headline={t("importModal.headline")}
      subtitle={t("importModal.subtitle")}
      onClose={onClose}
    >
      <Content>
        <Textarea
          rows={15}
          value={textInput}
          onChange={(e) => setTextInput(e.target.value)}
        />
        <Row>
          <IconButton icon="paste" onClick={onPasteClick}>
            {t("importModal.paste")}
          </IconButton>
          <PrimaryButton
            disabled={textInput.length === 0}
            onClick={onSubmitClick}
          >
            {t("importModal.submit")}
          </PrimaryButton>
        </Row>
      </Content>
    </Modal>
  );
};
