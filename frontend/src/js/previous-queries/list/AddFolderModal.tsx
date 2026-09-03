import { useState } from "react";
import { useTranslation } from "react-i18next";
import Modal from "../../modal/Modal";
import { Button } from "../../ui-components/Button";
import InputPlain from "../../ui-components/InputPlain/InputPlain";

interface Props {
  onClose: () => void;
  onSubmit: (folderName: string) => void;
  isValidName: (folderName: string) => boolean;
}

const AddFolderModal = ({ onClose, onSubmit, isValidName }: Props) => {
  const { t } = useTranslation();
  const [folderName, setFolderName] = useState<string>("");

  return (
    <Modal onClose={onClose} headline={t("addFolderModal.headline")}>
      <div className="max-w-[500px]">
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
            onChange={(value) => setFolderName((value as string | null) || "")}
            inputProps={{ autoFocus: true }}
          />
          <div className="mt-5 flex w-full items-center justify-between gap-5">
            <Button intent="secondary" onPress={onClose}>
              {t("common.cancel")}
            </Button>
            <Button
              intent="primary"
              type="submit"
              isDisabled={!isValidName(folderName)}
            >
              {t("common.create")}
            </Button>
          </div>
        </form>
      </div>
    </Modal>
  );
};

export default AddFolderModal;
