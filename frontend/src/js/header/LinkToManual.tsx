import styled from "@emotion/styled";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const List = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 5px;
`;

const SxIconButton = styled(IconButton)`
  padding: 6px 12px;
`;

interface Props {
  contactEmail?: string;
  manualUrl?: string;
}

export const LinkToManual = ({ contactEmail, manualUrl }: Props) => {
  const { t } = useTranslation();

  const Dropdown = useMemo(
    () => (
      <List>
        <a href={`mailto:${contactEmail}`} rel="noopener noreferrer">
          <IconButton fixedIconWidth={14} icon="paper-plane">
            {t("common.contact")}
          </IconButton>
        </a>
        <a href={manualUrl} target="_blank" rel="noopener noreferrer">
          <IconButton fixedIconWidth={14} icon="book">
            {t("common.manual")}
          </IconButton>
        </a>
      </List>
    ),
    [t, manualUrl, contactEmail],
  );
  return (
    <WithTooltip
      interactive
      trigger="click"
      arrow={false}
      html={Dropdown}
      offset={[-45, 5]}
    >
      <SxIconButton icon="question" frame />
    </WithTooltip>
  );
};
