import React, { FC } from "react";
import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { nodeHasActiveFilters } from "../model/node";

import IconButton from "../button/IconButton";
import type { ConceptQueryNodeType } from "../standard-query-editor/types";

const Container = styled("div")`
  text-transform: uppercase;
  padding: 10px 2px;
  white-space: nowrap;
`;

interface Props {
  node: ConceptQueryNodeType;
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
