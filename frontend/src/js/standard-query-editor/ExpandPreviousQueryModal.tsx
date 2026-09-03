import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import Modal from "../modal/Modal";
import { Button } from "../ui-components/Button";

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
          <Button intent="secondary" onPress={onClose} type="button">
            {t("common.cancel")}
          </Button>
          <Button intent="primary" autoFocus onPress={onAccept} type="submit">
            {t("expandPreviousQueryModal.submit")}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
export default ExpandPreviousQueryModal;
