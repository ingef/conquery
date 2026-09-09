import type { Dispatch, SetStateAction } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../api/types";
import InputMultiSelect from "../ui-components/InputMultiSelect/InputMultiSelect";
import { Modal, ModalBody, ModalHeader } from "../ui-components/Modal";

interface Props {
  onClose: () => void;
  entityStatusOptions: SelectOptionT[];
  setEntityStatusOptions: Dispatch<SetStateAction<SelectOptionT[]>>;
}

export const SettingsModal = ({
  onClose,
  setEntityStatusOptions,
  entityStatusOptions,
}: Props) => {
  const { t } = useTranslation();
  return (
    <Modal
      isOpen
      onOpenChange={(isOpen) => {
        if (!isOpen) onClose();
      }}
    >
      <ModalHeader>{t("history.settings.headline")}</ModalHeader>
      <ModalBody>
        <InputMultiSelect
          creatable
          label={t("history.settings.selectStatusHeadline")}
          placeholder={t("history.settings.selectStatusPlaceholder")}
          tooltip={t("history.settings.selectStatusTooltip")}
          onChange={setEntityStatusOptions}
          value={entityStatusOptions}
          options={entityStatusOptions}
        />
      </ModalBody>
    </Modal>
  );
};
