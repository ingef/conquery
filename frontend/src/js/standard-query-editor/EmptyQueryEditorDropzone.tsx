import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import FaIcon from "../icon/FaIcon";

const TextInitial = styled("div")`
  width: 100%;
  font-size: ${({ theme }) => theme.font.lg};
  padding: 30px;
  font-weight: 400;
  display: grid;
  grid-template-areas: "free headline" "arrow description";
  gap: 12px 20px;

  p {
    margin: 0;
  }
`;

const ArrowRight = styled(FaIcon)`
  font-size: 140px;
  color: ${({ theme }) => theme.col.grayLight};
  margin-left: -70px;
  grid-area: arrow;
`;
const Headline = styled("h2")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.huge};
  line-height: 1.3;
  grid-area: headline;
`;
const Grid = styled("div")`
  display: grid;
  gap: 5px;
  align-items: center;
  grid-template-columns: auto 1fr;
  margin-top: 10px;
`;
const Row = styled("div")`
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: flex-end;
`;

const Description = styled("div")`
  grid-area: description;
`;

const IconInABox = styled("div")`
  border: 1px solid ${({ theme }) => theme.col.grayLight};
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const SxFaIcon = styled(FaIcon)``;

export const EmptyQueryEditorDropzone = () => {
  const { t } = useTranslation();

  return (
    <TextInitial>
      <Headline>{t("dropzone.explanation")}</Headline>
      <ArrowRight icon="arrow-right" />
      <Description>
        <p>{t("dropzone.dropIntoThisArea")}</p>
        <Grid>
          <Row>
            <IconInABox>
              <SxFaIcon icon="folder" active />
            </IconInABox>
            <IconInABox>
              <SxFaIcon icon="minus" active />
            </IconInABox>
          </Row>
          {t("dropzone.aConcept")}
          <Row>
            <IconInABox>
              <SxFaIcon icon="diagram-project" active />
            </IconInABox>
          </Row>
          {t("dropzone.aQuery")}
          <Row>
            <IconInABox>
              <SxFaIcon icon="file" regular active />
            </IconInABox>
          </Row>
          {t("dropzone.aConceptList")}
        </Grid>
      </Description>
    </TextInitial>
  );
};
