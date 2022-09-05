import styled from "@emotion/styled";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const List = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 5px;
`;

const SxIconButton = styled(IconButton)`
  padding: 7px 12px;
`;

interface Props {
  contactEmail?: string;
  manualUrl?: string;
}

// Skidding makes Dropdown align the right edge with the button,
// might need to adjust this when adding more content.
const dropdownOffset: [number, number] = [-47, 5]; // [skidding, distance] / default [0, 10]

export const HelpMenu = ({ contactEmail, manualUrl }: Props) => {
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
      offset={dropdownOffset}
    >
      <SxIconButton icon="question" frame />
    </WithTooltip>
  );
};
