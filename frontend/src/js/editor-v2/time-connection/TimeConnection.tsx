import styled from "@emotion/styled";
import { DOMAttributes, memo } from "react";
import { useTranslation } from "react-i18next";

import { TreeChildrenTime } from "../types";
import {
  useGetNodeLabel,
  useGetTranslatedTimestamp,
  useTranslatedInterval,
  useTranslatedOperator,
} from "../util";

const Container = styled("div")`
  margin: 0 auto;
  display: inline-flex;
  flex-direction: column;
  user-select: none;
`;

const Row = styled("div")`
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: ${({ theme }) => theme.font.sm};
`;

const ConceptName = styled("span")`
  font-weight: bold;
  color: ${({ theme }) => theme.col.blueGrayDark};
`;
const Timestamp = styled("span")`
  font-weight: bold;
  color: ${({ theme }) => theme.col.palette[0]};
`;
const Interval = styled("span")`
  font-weight: bold;
  color: ${({ theme }) => theme.col.palette[1]};
`;
const Operator = styled("span")`
  font-weight: bold;
  color: ${({ theme }) => theme.col.palette.at(-2)};
`;

export const TimeConnection = memo(
  ({
    conditions,
    onDoubleClick,
  }: {
    conditions: TreeChildrenTime;
    onDoubleClick: DOMAttributes<HTMLElement>["onDoubleClick"];
  }) => {
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
      <Container onDoubleClick={onDoubleClick}>
        <Row>
          <Timestamp>{aTimestamp}</Timestamp>
          <span>{t("editorV2.dateRangeFrom")}</span>
          <ConceptName>{a}</ConceptName>
        </Row>
        <Row>
          {conditions.operator !== "while" && <Interval>{interval}</Interval>}
          <Operator>{operator}</Operator>
        </Row>
        <Row>
          <Timestamp>{bTimestamp}</Timestamp>
          <span>{t("editorV2.dateRangeFrom")}</span>
          <ConceptName>{b}</ConceptName>
        </Row>
      </Container>
    );
  },
);
