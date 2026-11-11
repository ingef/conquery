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

const AddFolderModalContent = ({
  onSubmit,
  isValidName,
  close,
}: Props & { close: () => void }) => {
  const { t } = useTranslation();
  const [folderName, setFolderName] = useState<string>("");

  return (
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
  );
};

const AddFolderModal = (props: Props) => (
  <Modal>
    {({ close }) => <AddFolderModalContent {...props} close={close} />}
  </Modal>
);

export default AddFolderModal;
