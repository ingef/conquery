import styled from "@emotion/styled";
import { Dispatch, SetStateAction } from "react";
import { useTranslation } from "react-i18next";

import { SelectOptionT } from "../api/types";
import Modal from "../modal/Modal";
import InputMultiSelect from "../ui-components/InputMultiSelect/InputMultiSelect";

const Content = styled("div")`
  width: 300px;
`;

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
    <Modal onClose={onClose} headline={t("history.settings.headline")}>
      <Content>
        <InputMultiSelect
          creatable
          label={t("history.settings.selectStatusHeadline")}
          placeholder={t("history.settings.selectStatusPlaceholder")}
          tooltip={t("history.settings.selectStatusTooltip")}
          onChange={setEntityStatusOptions}
          value={entityStatusOptions}
          options={entityStatusOptions}
        />
      </Content>
    </Modal>
  );
};
