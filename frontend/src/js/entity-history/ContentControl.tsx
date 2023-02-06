import styled from "@emotion/styled";
import { memo, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const Root = styled("div")`
  display: flex;
  flex-direction: column;
  align-items: center;
`;

export type ContentType =
  | "groupId"
  | "secondaryId"
  | "money"
  | "concept"
  | "rest"
  | "dates";
export type ContentFilterValue = Record<ContentType, boolean>;

interface Props {
  value: ContentFilterValue;
  onChange: (value: ContentFilterValue) => void;
}

const ContentControl = ({ value, onChange }: Props) => {
  const { t } = useTranslation();

  const options = useMemo(
    () => [
      {
        key: "money" as const,
        icon: "euro-sign" as const,
        tooltip: t("history.content.money"),
      },
      {
        key: "concept" as const,
        icon: "folder" as const,
        tooltip: t("history.content.concept"),
      },
      {
        key: "rest" as const,
        icon: "info" as const,
        tooltip: t("history.content.rest"),
      },
      {
        key: "groupId" as const,
        icon: "fingerprint" as const,
        tooltip: t("history.content.fingerprint"),
      },
    ],
    [t],
  );

  return (
    <Root>
      {options.map((option) => {
        const active = value[option.key];
        return (
          <WithTooltip key={option.key} text={option.tooltip}>
            <IconButton
              icon={option.icon}
              active={active}
              light={!active}
              onClick={() => {
                onChange({ ...value, [option.key]: !value[option.key] });
              }}
            />
          </WithTooltip>
        );
      })}
    </Root>
  );
};

export const useContentControl = () => {
  const [contentFilter, setContentFilter] = useState<ContentFilterValue>({
    groupId: false,
    secondaryId: true,
    concept: true,
    money: true,
    rest: false,
    dates: true,
  });

  return {
    contentFilter,
    setContentFilter,
  };
};

export default memo(ContentControl);
