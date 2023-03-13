import styled from "@emotion/styled";
import { Dispatch, memo, SetStateAction, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import { SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";
import IconButton from "../button/IconButton";
import ProgressBar from "../common/components/ProgressBar";
import { Heading3 } from "../headings/Headings";
import WithTooltip from "../tooltip/WithTooltip";

import { SettingsModal } from "./SettingsModal";

const Root = styled("div")`
  display: grid;
  gap: 8px;
  background-color: white;
  box-shadow: 1px 1px 5px 0px rgba(0, 0, 0, 0.2);
  padding: 14px;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const BaseInfo = styled("div")`
  display: flex;
  gap: 15px;
  justify-content: space-between;
  overflow: hidden;
`;

const SxHeading3 = styled(Heading3)`
  flex-shrink: 0;
  margin: 0;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
`;

const Count = styled(SxHeading3)`
  justify-self: end;
`;
const Text = styled("span")`
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme }) => theme.col.gray};
  text-transform: uppercase;
  font-weight: 300;
`;

const SpecialText = styled("p")<{ zero?: boolean }>`
  margin: 0;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme, zero }) => (zero ? theme.col.red : theme.col.gray)};
  text-transform: uppercase;
  font-weight: 400;
`;

const StatsGrid = styled("div")`
  display: grid;
  gap: 0px 4px;
  grid-template-columns: auto 1fr;
`;

interface Props {
  className?: string;
  idsCount: number;
  markedCount: number;
  entityStatusOptions: SelectOptionT[];
  setEntityStatusOptions: Dispatch<SetStateAction<SelectOptionT[]>>;
}
export const NavigationHeader = memo(
  ({
    className,
    idsCount,
    markedCount,
    setEntityStatusOptions,
    entityStatusOptions,
  }: Props) => {
    const { t } = useTranslation();
    const label = useSelector<StateT, string>(
      (state) => state.entityHistory.label,
    );

    const [settingsModalOpen, setSettingsModalOpen] = useState(false);

    return (
      <Root className={className}>
        {settingsModalOpen && (
          <SettingsModal
            onClose={() => setSettingsModalOpen(false)}
            setEntityStatusOptions={setEntityStatusOptions}
            entityStatusOptions={entityStatusOptions}
          />
        )}
        <BaseInfo>
          <div style={{ overflow: "hidden" }}>
            <SxHeading3 title={label}>{label}</SxHeading3>
            <SpecialText>{t("history.history")}</SpecialText>
          </div>
          <WithTooltip text={t("history.settings.headline")}>
            <IconButton
              icon="sliders"
              onClick={() => setSettingsModalOpen(true)}
            />
          </WithTooltip>
        </BaseInfo>
        <StatsGrid>
          <Count>{idsCount}</Count>
          <Text>{t("tooltip.entitiesFound", { count: idsCount })}</Text>
          <Count>{markedCount}</Count>
          <Text>{t("history.marked", { count: markedCount })}</Text>
        </StatsGrid>
        <ProgressBar donePercent={100 * (markedCount / idsCount)} />
      </Root>
    );
  },
);
