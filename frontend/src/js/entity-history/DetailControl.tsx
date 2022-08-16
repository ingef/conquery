import styled from "@emotion/styled";
import { IconName } from "@fortawesome/fontawesome-svg-core";
import { Dispatch, memo, SetStateAction, useMemo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const Root = styled("div")`
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
`;

export type DetailLevel = "summary" | "detail" | "full";

interface Props {
  className?: string;
  detailLevel: DetailLevel;
  setDetailLevel: Dispatch<SetStateAction<DetailLevel>>;
}

const useButtonConfig = () => {
  const { t } = useTranslation();
  return useMemo(
    (): {
      icon: IconName;
      value: string;
      tooltip: string;
    }[] => [
      {
        icon: "circle",
        value: "summary",
        tooltip: t("history.detail.summary"),
      },
      {
        icon: "circle-dot",
        value: "detail",
        tooltip: t("history.detail.detail"),
      },
      {
        icon: "bullseye",
        value: "full",
        tooltip: t("history.detail.full"),
      },
    ],
    [t],
  );
};

export const DetailControl = memo(
  ({ className, detailLevel, setDetailLevel }: Props) => {
    const navOptions = useButtonConfig();
    return (
      <Root className={className}>
        {navOptions.map(({ value, icon, tooltip }) => {
          const selected = value === detailLevel;

          return (
            <WithTooltip key={value} text={tooltip}>
              <IconButton
                key={value}
                onClick={() => setDetailLevel(value as DetailLevel)}
                icon={icon}
                active={selected}
              />
            </WithTooltip>
          );
        })}
      </Root>
    );
  },
);
