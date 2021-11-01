import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

const Root = styled("div")`
  position: relative;
  display: flex;
  width: 100%;
  flex-direction: column;
  margin-left: 10px;
`;

const MsgContainer = styled("div")`
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
`;

const Msg = styled("div")`
  width: 400px;
  white-space: initial;
`;

const Message = styled("p")`
  font-size: ${({ theme }) => theme.font.lg};
  margin: 10px 0 0;
  font-weight: 400;
`;

const SubMessage = styled("p")`
  font-size: ${({ theme }) => theme.font.md};
  margin: 0 0 10px;
`;

const Preview = styled("div")`
  border-radius: ${({ theme }) => theme.borderRadius};
  background-color: ${({ theme }) => theme.col.grayVeryLight};
  height: 20px;
  margin: 3px 0;
`;

const Container = styled("div")`
  padding-left: 20px;
  display: flex;
  flex-direction: column;
`;

const EmptyConceptTreeList = () => {
  const { t } = useTranslation();

  return (
    <Root>
      <MsgContainer>
        <Msg>
          <Message>{t("conceptTreeList.noTrees")}</Message>
          <SubMessage>{t("conceptTreeList.noTreesExplanation")}</SubMessage>
        </Msg>
      </MsgContainer>
      <Preview style={{ width: `${200}px` }} />
      <Preview style={{ width: `${100}px` }} />
      <Container>
        <Preview style={{ width: `${250}px` }} />
        <Preview style={{ width: `${150}px` }} />
        <Preview style={{ width: `${300}px` }} />
        <Container>
          <Preview style={{ width: `${200}px` }} />
          <Preview style={{ width: `${50}px` }} />
        </Container>
      </Container>
      <Preview style={{ width: `${350}px` }} />
      <Preview style={{ width: `${200}px` }} />
      <Preview style={{ width: `${300}px` }} />
      <Preview style={{ width: `${250}px` }} />
    </Root>
  );
};

export default EmptyConceptTreeList;
