import styled from "@emotion/styled";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { nodeHasActiveFilters } from "../model/node";
import type { StandardQueryNodeT } from "../standard-query-editor/types";
import WithTooltip from "../tooltip/WithTooltip";

const SxWithTooltip = styled(WithTooltip)`
  text-transform: uppercase;
  white-space: nowrap;
`;

interface Props {
  node: StandardQueryNodeT;
  compact?: boolean;
  onResetAllFilters: () => void;
}

const ResetAllFiltersButton: FC<Props> = ({
  node,
  compact,
  onResetAllFilters,
}) => {
  const { t } = useTranslation();

  if (!nodeHasActiveFilters(node)) return null;

  return (
    <SxWithTooltip
      text={compact ? t("queryNodeEditor.resetSettings") : undefined}
    >
      <IconButton active onClick={onResetAllFilters} icon="undo">
        {!compact && t("queryNodeEditor.resetSettings")}
      </IconButton>
    </SxWithTooltip>
  );
};

export default ResetAllFiltersButton;
