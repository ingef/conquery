import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { DestroyButton } from "../button/DestroyButton";
import { TransparentButton } from "../button/TransparentButton";

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
          <TransparentButton className="mx-[10px]" onClick={onClose}>
            {t("common.cancel")}
          </TransparentButton>
          <DestroyButton onClick={onDelete}>{t("common.delete")}</DestroyButton>
        </div>
      </div>
    </Modal>
  );
};

export default DeleteModal;
