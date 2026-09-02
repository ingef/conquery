import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import PrimaryButton from "../button/PrimaryButton";
import { TransparentButton } from "../button/TransparentButton";
import Modal from "../modal/Modal";

const description = tv({
  base: ["max-w-[400px]", "mb-5"],
});

const ExpandPreviousQueryModal = ({
  onClose,
  onAccept,
  className,
}: {
  className?: string;
  onClose: () => void;
  onAccept: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <Modal
      className={className}
      onClose={onClose}
      headline={t("expandPreviousQueryModal.headline")}
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          onAccept();
        }}
      >
        <p className={description()}>
          {t("expandPreviousQueryModal.description")}
        </p>
        <div className="flex items-center justify-between">
          <TransparentButton onClick={onClose} type="button">
            {t("common.cancel")}
          </TransparentButton>
          <PrimaryButton autoFocus onClick={onAccept} type="submit">
            {t("expandPreviousQueryModal.submit")}
          </PrimaryButton>
        </div>
      </form>
    </Modal>
  );
};
export default ExpandPreviousQueryModal;
