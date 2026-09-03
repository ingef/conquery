import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";

import Modal from "./Modal";

const buttons = tv({
  base: ["flex items-center justify-center", "gap-[15px]"],
});

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
      <div className="max-w-[400px]">
        {description && <p className="mb-5">{description}</p>}
        <div className={buttons()}>
          <div className="mx-[10px]">
            <Button intent="secondary" onPress={onClose}>
              {t("common.cancel")}
            </Button>
          </div>
          <Button intent="danger" onPress={onDelete}>
            {t("common.delete")}
          </Button>
        </div>
      </div>
    </Modal>
  );
};

export default DeleteModal;
