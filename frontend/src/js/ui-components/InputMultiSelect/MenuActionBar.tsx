import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import { TransparentButton } from "../../button/TransparentButton";
import { exists } from "../../common/helpers/exists";

const Row = styled("div")`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 10px;
  border-bottom: 1px solid #ccc;
`;

const InfoText = styled("p")`
  margin: 0;
  color: ${({ theme }) => theme.col.gray};
  font-size: ${({ theme }) => theme.font.xs};
  margin-right: 10px;
`;

interface Props {
  optionsCount: number;
  total?: number;
  onInsertAllClick: () => void;
}

const MenuActionBar = ({ optionsCount, total, onInsertAllClick }: Props) => {
  const { t } = useTranslation();

  return (
    <Row>
      <InfoText>
        {t("inputMultiSelect.options", { count: optionsCount })}
        {exists(total) &&
          total !== optionsCount &&
          t("inputMultiSelect.ofTotal", { count: total })}
      </InfoText>
      <TransparentButton
        tiny
        disabled={optionsCount === 0}
        onClick={onInsertAllClick}
      >
        {t("inputMultiSelect.insertAll")}
      </TransparentButton>
    </Row>
  );
};

export default MenuActionBar;
