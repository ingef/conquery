import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import { TransparentButton } from "../button/TransparentButton";

const Root = styled("div")`
  height: 100%;
  width: 100%;
  padding: 20px;
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
`;

const Heading = styled("h3")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.md};
`;
const Description = styled("p")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.sm};
  max-width: 300px;
  text-align: justify;
`;

const ReloadButton = styled(TransparentButton)`
  margin-top: 10px;
`;

const ErrorFallback = ({
  allowFullRefresh,
  onReset,
}: {
  allowFullRefresh?: boolean;
  onReset?: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <Root>
      <Heading>{t("error.sorry")}</Heading>
      <Description>{t("error.description")}</Description>
      {allowFullRefresh && (
        <>
          <Description>{t("error.reloadDescription")}</Description>
          <ReloadButton onClick={() => window.location.reload()}>
            {t("error.reload")}
          </ReloadButton>
        </>
      )}
      {onReset && (
        <>
          <Description>{t("error.resetDescription")}</Description>
          <ReloadButton onClick={onReset}>{t("error.reset")}</ReloadButton>
        </>
      )}
    </Root>
  );
};
export default ErrorFallback;
