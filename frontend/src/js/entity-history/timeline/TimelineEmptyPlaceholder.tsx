import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import { StateT } from "../../app/reducers";
import FaIcon from "../../icon/FaIcon";
import { EntityHistoryStateT } from "../reducer";

const Root = styled("div")`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  width: 100%;
  font-weight: 400;
  color: ${({ theme }) => theme.col.gray};
`;

const Message = styled("p")`
  color: ${({ theme }) => theme.col.black};
  font-size: ${({ theme }) => theme.font.lg};
  margin: 10px 0 0;
  font-weight: 400;
`;

const BigIcon = styled(FaIcon)`
  font-size: 120px;
  color: ${({ theme }) => theme.col.grayLight};
`;

const Headline = styled("h2")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.huge};
  line-height: 1.3;
`;

const Row = styled("p")`
  display: flex;
  gap: 30px;
  align-items: center;
`;

const Description = styled("p")`
  font-size: ${({ theme }) => theme.font.lg};
  margin: 0;
`;

export const TimelineEmptyPlaceholder = () => {
  const { t } = useTranslation();

  const ids = useSelector<StateT, EntityHistoryStateT["entityIds"]>(
    (state) => state.entityHistory.entityIds,
  );
  const id = useSelector<StateT, EntityHistoryStateT["currentEntityId"]>(
    (state) => state.entityHistory.currentEntityId,
  );

  return (
    <Root>
      <Row>
        <BigIcon icon="list-ul" />
        <div>
          <Headline>{t("history.emptyTimeline.headline")}</Headline>
          <Description>{t("history.emptyTimeline.description")}</Description>
          <Message>
            {ids.length === 0
              ? t("history.emptyTimeline.descriptionWithoutIds")
              : id
              ? t("history.emptyTimeline.descriptionWithId")
              : t("history.emptyTimeline.descriptionWithoutId")}
          </Message>
        </div>
      </Row>
    </Root>
  );
};
