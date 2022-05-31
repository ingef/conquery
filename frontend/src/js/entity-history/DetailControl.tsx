import styled from "@emotion/styled";
import { Dispatch, ReactNode, SetStateAction, useMemo } from "react";
import { useTranslation } from "react-i18next";

import FaIcon from "../icon/FaIcon";
import SmallTabNavigation from "../small-tab-navigation/SmallTabNavigation";

const Root = styled("div")`
  display: flex;
  align-items: center;
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
      label: ({ selected }: { selected?: boolean }) => ReactNode;
      value: string;
      tooltip: string;
    }[] => [
      {
        label: ({ selected }) => (
          <FaIcon active={selected} gray={!selected} icon="circle" />
        ),
        value: "summary",
        tooltip: t("history.detail.summary"),
      },
      {
        label: ({ selected }) => (
          <FaIcon active={selected} gray={!selected} icon="circle-dot" />
        ),
        value: "detail",
        tooltip: t("history.detail.detail"),
      },
      {
        label: ({ selected }) => (
          <FaIcon active={selected} gray={!selected} icon="bullseye" />
        ),
        value: "full",
        tooltip: t("history.detail.full"),
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
  const navOptions = useButtonConfig();
  return (
    <Root className={className}>
      <SmallTabNavigation
        size="L"
        options={navOptions}
        selectedTab={detailLevel}
        onSelectTab={(tab) => setDetailLevel(tab as DetailLevel)}
      />
    </Root>
  );
};
