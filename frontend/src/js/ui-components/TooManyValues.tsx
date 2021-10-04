import styled from "@emotion/styled";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";

const Root = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 6px 0px 3px;
`;

const Text = styled("p")`
  margin: 0;
`;

interface Props {
  count: number;
  onClear: () => void;
}

const TooManyValues: FC<Props> = ({ count, onClear }) => {
  const { t } = useTranslation();

  return (
    <Root>
      <Text>{t("queryNodeEditor.tooManyValues", { count })}</Text>
      <IconButton
        icon="times"
        title={t("common.clearValue")}
        aria-label={t("common.clearValue")}
        onClick={onClear}
      />
    </Root>
  );
};

export default TooManyValues;
