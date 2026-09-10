import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "./Button";
import { Modal, ModalBody, ModalFooter, ModalHeader } from "./Modal";

interface PropsType {
  headline: ReactNode;
  description?: ReactNode;
  /** the modal closes once this resolves */
  onDelete: () => Promise<unknown>;
}

export const DeleteModal = ({ headline, description, onDelete }: PropsType) => {
  const { t } = useTranslation();

  return (
    <Modal size="sm">
      {({ close }) => (
        <>
          <ModalHeader>{headline}</ModalHeader>
          {description && (
            <ModalBody>
              <p>{description}</p>
            </ModalBody>
          )}
          <ModalFooter>
            <Button slot="close">{t("common.cancel")}</Button>
            <Button
              danger
              onPress={async () => {
                await onDelete();
                close();
              }}
            >
              {t("common.delete")}
            </Button>
          </ModalFooter>
        </>
      )}
    </Modal>
  );
};
