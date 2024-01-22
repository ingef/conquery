import styled from "@emotion/styled";
import { faTimes } from "@fortawesome/free-solid-svg-icons";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";

const Root = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 3px 10px;
  border: 1px solid ${({ theme }) => theme.col.gray};
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const Text = styled("p")`
  margin: 0;
  line-height: 1;
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
        icon={faTimes}
        tiny
        title={t("common.clearValue")}
        aria-label={t("common.clearValue")}
        onClick={onClear}
      />
    </Root>
  );
};

export default TooManyValues;
