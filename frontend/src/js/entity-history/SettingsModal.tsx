import type { Dispatch, SetStateAction } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../api/types";
import { ComboBoxMultiField } from "../ui-components/ComboBoxMultiField";
import { Modal, ModalBody, ModalHeader } from "../ui-components/Modal";

interface Props {
  entityStatusOptions: SelectOptionT[];
  setEntityStatusOptions: Dispatch<SetStateAction<SelectOptionT[]>>;
}

export const SettingsModal = ({
  setEntityStatusOptions,
  entityStatusOptions,
}: Props) => {
  const { t } = useTranslation();
  return (
    <Modal>
      <ModalHeader>{t("history.settings.headline")}</ModalHeader>
      <ModalBody>
        <ComboBoxMultiField
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
