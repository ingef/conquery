import styled from "@emotion/styled";
import { faBook } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";

const Root = styled("div")`
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 7px;
`;

const Description = styled("p")`
  margin: 0 10px;
  font-size: ${({ theme }) => theme.font.md};
`;

const SxIconButton = styled(IconButton)`
  justify-content: center;
  width: 100%;
`;

interface Props {
  description: string;
  className?: string;
  manualUrl?: string;
}

const FormHeader = ({ className, description, manualUrl }: Props) => {
  const { t } = useTranslation();
  return (
    <Root className={className}>
      <Description>{description}</Description>
      {manualUrl && (
        <a href={manualUrl} target="_blank" rel="noreferrer">
          <SxIconButton frame icon={faBook}>
            {t("externalForms.manualButton")}
          </SxIconButton>
        </a>
      )}
    </Root>
  );
};

export default FormHeader;
