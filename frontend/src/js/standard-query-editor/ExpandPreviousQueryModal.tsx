import { useTranslation } from "react-i18next";
import { Button } from "../ui-components/Button";
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from "../ui-components/Modal";

const ExpandPreviousQueryModal = ({
  onClose,
  onAccept,
}: {
  onClose: () => void;
  onAccept: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <Modal
      isOpen
      onOpenChange={(isOpen) => {
        if (!isOpen) onClose();
      }}
    >
      <ModalHeader>{t("expandPreviousQueryModal.headline")}</ModalHeader>
      <form
        className="flex flex-col gap-5"
        onSubmit={(e) => {
          e.preventDefault();
          onAccept();
        }}
      >
        <ModalBody>
          <p className="max-w-[400px]">
            {t("expandPreviousQueryModal.description")}
          </p>
        </ModalBody>
        <ModalFooter>
          <Button slot="close">{t("common.cancel")}</Button>
          <Button intent="primary" autoFocus type="submit">
            {t("expandPreviousQueryModal.submit")}
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
};
export default ExpandPreviousQueryModal;
