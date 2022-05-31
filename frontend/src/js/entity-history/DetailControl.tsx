import styled from "@emotion/styled";
import { IconName } from "@fortawesome/fontawesome-svg-core";
import { Dispatch, SetStateAction, useMemo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const Root = styled("div")`
  display: flex;
  align-items: center;
`;
const SxIconButton = styled(IconButton)`
  display: grid;
  gap: 10px;
  grid-template-columns: 1fr;
  padding: 2px 4px;
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
    () => [
      {
        icon: "circle",
        key: "summary",
        text: t("history.detail.summary"),
      },
      {
        icon: "circle-dot",
        key: "detail",
        text: t("history.detail.detail"),
      },
      {
        icon: "bullseye",
        key: "full",
        text: t("history.detail.full"),
      },
    ],
    [t],
  );
};

export const DetailControl = ({
  className,
  detailLevel,
  setDetailLevel,
}: Props) => {
  const buttonConfig = useButtonConfig();
  return (
    <Root className={className}>
      {buttonConfig.map(({ icon, key, text }) => {
        const active = key === detailLevel;

        return (
          <WithTooltip text={text}>
            <SxIconButton
              active={active}
              icon={icon as IconName}
              onClick={() => setDetailLevel(key as DetailLevel)}
            />
          </WithTooltip>
        );
      })}
    </Root>
  );
};
