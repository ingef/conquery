import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "./Button";
import { Modal, ModalBody, ModalFooter, ModalHeader } from "./Modal";

interface PropsType {
  headline: ReactNode;
  description?: ReactNode;
  onClose: () => void;
  onDelete: () => Promise<unknown>;
}

export const DeleteModal = ({
  headline,
  description,
  onClose,
  onDelete,
}: PropsType) => {
  const { t } = useTranslation();

  return (
    <Modal
      size="sm"
      isOpen
      onOpenChange={(isOpen) => {
        if (!isOpen) onClose();
      }}
    >
      <ModalHeader>{headline}</ModalHeader>
      {description && (
        <ModalBody>
          <p>{description}</p>
        </ModalBody>
      )}
      <ModalFooter>
        <Button slot="close">{t("common.cancel")}</Button>
        <Button danger onPress={onDelete}>
          {t("common.delete")}
        </Button>
      </ModalFooter>
    </Modal>
  );
};
