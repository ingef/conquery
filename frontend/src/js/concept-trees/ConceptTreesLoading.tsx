import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import FaIcon from "../icon/FaIcon";

const Container = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 5px 12px;
`;

const StyledFaIcon = styled(FaIcon)`
  margin-right: 10px;
`;

const ConceptTreesLoading = () => {
  const { t } = useTranslation();

  return (
    <Container>
      <StyledFaIcon icon="spinner" />
      <span>{t("conceptTreeList.loading")}</span>
    </Container>
  );
};

export default ConceptTreesLoading;
