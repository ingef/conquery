import styled from "@emotion/styled";
import { memo, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const Root = styled("div")`
  display: flex;
  gap: 3px;
`;

const SxIconButton = styled(IconButton)`
  width: 24px;
`;

const BottomBorder = styled("div")<{ active?: boolean }>`
  width: 100%;
  height: 3px;
  background-color: ${({ theme, active }) =>
    active ? theme.col.black : theme.col.grayLight};
`;

export type ContentFilterValue = Record<
  "secondaryId" | "concept" | "rest",
  boolean
>;

interface Props {
  value: ContentFilterValue;
  onChange: (value: ContentFilterValue) => void;
}

const ContentControl = ({ value, onChange }: Props) => {
  const { t } = useTranslation();

  const options = useMemo(
    () => [
      {
        key: "secondaryId" as const,
        icon: "microscope" as const,
        tooltip: t("history.content.secondaryId"),
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
    ],
    [t],
  );

  return (
    <Root>
      {options.map((option) => {
        const active = value[option.key];
        return (
          <WithTooltip text={option.tooltip}>
            <SxIconButton
              tiny
              icon={option.icon}
              key={option.key}
              active={active}
              onClick={() => {
                onChange({ ...value, [option.key]: !value[option.key] });
              }}
            />
            <BottomBorder active={active} />
          </WithTooltip>
        );
      })}
    </Root>
  );
};

export const useContentControl = () => {
  const [contentFilter, setContentFilter] = useState<ContentFilterValue>({
    secondaryId: true,
    concept: true,
    rest: true,
  });

  return {
    contentFilter,
    setContentFilter,
  };
};

export default memo(ContentControl);
