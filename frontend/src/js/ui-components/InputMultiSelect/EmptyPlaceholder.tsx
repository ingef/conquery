import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

const Container = styled("div")`
  width: 100%;
  padding: 10px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${({ theme }) => theme.col.gray};
  font-size: ${({ theme }) => theme.font.sm};
`;

const EmptyPlaceholder = () => {
  const { t } = useTranslation();
  return <Container>{t("inputSelect.empty")}</Container>;
};

export default EmptyPlaceholder;
