import styled from "@emotion/styled";
import React from "react";
import { useTranslation } from "react-i18next";

const Text = styled("div")`
  font-size: ${({ theme }) => theme.font.tiny};
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.gray};
  padding-right: 6px;
  font-weight: 700;
`;

const Optional = () => {
  const { t } = useTranslation();

  return <Text>({t("common.optional")})</Text>;
};

export default Optional;
