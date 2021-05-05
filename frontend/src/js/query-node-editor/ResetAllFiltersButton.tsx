import styled from "@emotion/styled";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { nodeHasActiveFilters } from "../model/node";
import type { StandardQueryNodeT } from "../standard-query-editor/types";

const Container = styled("div")`
  text-transform: uppercase;
  white-space: nowrap;
`;

interface Props {
  node: StandardQueryNodeT;
  onResetAllFilters: () => void;
}

const ResetAllFiltersButton: FC<Props> = ({ node, onResetAllFilters }) => {
  const { t } = useTranslation();

  if (!nodeHasActiveFilters(node)) return null;

  return (
    <Container>
      <IconButton active onClick={onResetAllFilters} icon="undo">
        {t("queryNodeEditor.resetSettings")}
      </IconButton>
    </Container>
  );
};

export default ResetAllFiltersButton;
