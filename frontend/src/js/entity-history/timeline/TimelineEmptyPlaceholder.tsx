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
`;

const Message = styled("p")`
  font-size: ${({ theme }) => theme.font.lg};
  margin: 10px 0 0;
  font-weight: 400;
`;

const BigIcon = styled(FaIcon)`
  font-size: 140px;
  color: ${({ theme }) => theme.col.grayLight};
`;

const Description = styled("p")`
  font-size: ${({ theme }) => theme.font.lg};
  margin: 0;
`;

export const TimelineEmptyPlaceholder = () => {
  const { t } = useTranslation();

  const id = useSelector<StateT, EntityHistoryStateT["currentEntityId"]>(
    (state) => state.entityHistory.currentEntityId,
  );

  return (
    <Root>
      <Message>{t("history.emptyTimeline.headline")}</Message>
      <p>
        {id
          ? t("history.emptyTimeline.descriptionWithId")
          : t("history.emptyTimeline.descriptionWithoutId")}
      </p>
      <BigIcon icon="list-ul" />
    </Root>
  );
};
