import styled from "@emotion/styled";
import TransparentButton from "js/button/TransparentButton";
import { useTranslation } from "react-i18next";

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
  onInsertAllClick: () => void;
}

const MenuActionBar = ({ optionsCount, onInsertAllClick }: Props) => {
  const { t } = useTranslation();
  return (
    <Row>
      <InfoText>
        {optionsCount} {t("inputMultiSelect.options")}
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
