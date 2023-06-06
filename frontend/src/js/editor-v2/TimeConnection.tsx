import styled from "@emotion/styled";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import { TreeChildrenTime } from "./types";
import {
  useGetNodeLabel,
  useGetTranslatedTimestamp,
  useTranslatedInterval,
  useTranslatedOperator,
} from "./util";

const TimeConnectionContainer = styled("div")`
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: ${({ theme }) => theme.font.xs};
`;

const ConceptName = styled("span")`
  font-weight: bold;
  color: ${({ theme }) => theme.col.blueGrayDark};
`;
const Timestamp = styled("span")`
  font-weight: bold;
  color: ${({ theme }) => theme.col.palette[6]};
`;
const Interval = styled("span")`
  font-weight: bold;
  color: ${({ theme }) => theme.col.orange};
`;
const Operator = styled("span")`
  font-weight: bold;
  color: ${({ theme }) => theme.col.green};
`;

export const TimeConnection = memo(
  ({ conditions }: { conditions: TreeChildrenTime }) => {
    const { t } = useTranslation();
    const getNodeLabel = useGetNodeLabel();
    const getTranslatedTimestamp = useGetTranslatedTimestamp();

    const aTimestamp = getTranslatedTimestamp(conditions.timestamps[0]);
    const bTimestamp = getTranslatedTimestamp(conditions.timestamps[1]);
    const a = getNodeLabel(conditions.items[0]);
    const b = getNodeLabel(conditions.items[1]);
    const operator = useTranslatedOperator(conditions.operator);
    const interval = useTranslatedInterval(conditions.interval);

    return (
      <div>
        <TimeConnectionContainer>
          <Timestamp>{aTimestamp}</Timestamp>
          <span>{t("editorV2.dateRangeFrom")}</span>
          <ConceptName>{a}</ConceptName>
        </TimeConnectionContainer>
        <TimeConnectionContainer>
          <Interval>{interval}</Interval>
          <Operator>{operator}</Operator>
        </TimeConnectionContainer>
        <TimeConnectionContainer>
          <Timestamp>{bTimestamp}</Timestamp>
          <span>{t("editorV2.dateRangeFrom")}</span>
          <ConceptName>{b}</ConceptName>
        </TimeConnectionContainer>
      </div>
    );
  },
);
