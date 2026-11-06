import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "../../ui-components/Button";
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from "../../ui-components/Modal";
import { TextField } from "../../ui-components/TextField";

interface Props {
  onSubmit: (folderName: string) => void;
  isValidName: (folderName: string) => boolean;
}

const AddFolderModal = ({ onSubmit, isValidName }: Props) => {
  const { t } = useTranslation();
  const [folderName, setFolderName] = useState<string>("");

  return (
    <Modal>
      {({ close }) => (
        <>
          <ModalHeader>{t("addFolderModal.headline")}</ModalHeader>
          <form
            className="flex flex-col gap-5"
            onSubmit={(e) => {
              e.preventDefault();
              onSubmit(folderName);
              close();
            }}
          >
            <ModalBody>
              <div className="flex flex-col gap-5">
                <p>{t("addFolderModal.description")}</p>
                <TextField
                  label={t("addFolderModal.inputLabel")}
                  value={folderName}
                  onChange={setFolderName}
                  autoFocus
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
        </>
      )}
    </Modal>
  );
};

export default AddFolderModal;
