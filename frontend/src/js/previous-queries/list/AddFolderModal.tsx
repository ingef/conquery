import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "../../ui-components/Button";
import InputPlain from "../../ui-components/InputPlain/InputPlain";
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from "../../ui-components/Modal";

interface Props {
  onClose: () => void;
  onSubmit: (folderName: string) => void;
  isValidName: (folderName: string) => boolean;
}

const AddFolderModal = ({ onClose, onSubmit, isValidName }: Props) => {
  const { t } = useTranslation();
  const [folderName, setFolderName] = useState<string>("");

  return (
    <Modal
      isOpen
      onOpenChange={(isOpen) => {
        if (!isOpen) onClose();
      }}
    >
      <ModalHeader>{t("addFolderModal.headline")}</ModalHeader>
      <form
        className="flex flex-col gap-5"
        onSubmit={(e) => {
          e.preventDefault();
          onSubmit(folderName);
        }}
      >
        <ModalBody>
          <div className="flex max-w-[500px] flex-col gap-5">
            <p>{t("addFolderModal.description")}</p>
            <InputPlain
              label={t("addFolderModal.inputLabel")}
              value={folderName}
              inputType="text"
              onChange={(value) =>
                setFolderName((value as string | null) || "")
              }
              inputProps={{ autoFocus: true }}
            />
          </div>
        </ModalBody>
        <ModalFooter>
          <Button slot="close">{t("common.cancel")}</Button>
          <Button
            intent="primary"
            type="submit"
            isDisabled={!isValidName(folderName)}
          >
            {t("common.create")}
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
};

export default AddFolderModal;
