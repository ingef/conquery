import React, { FC } from "react";
import { ColumnDescriptionType } from "./Preview";
import styled from "@emotion/styled";

const Stat = styled("div")`
  margin: 0 0 10px;
`;
const Name = styled("div")`
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.sm};
`;
const Strong = styled("span")`
  font-style: italic;
`;
const Value = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
`;

interface Props {
  colName: string;
  columnType: ColumnDescriptionType;
  rawColumnData: string[];
}

const ColumnStats: FC<Props> = ({ colName, columnType, rawColumnData }) => {
  switch (columnType) {
    case "DATE":
    case "DATE_RANGE": {
      return <div>DATE Stats</div>;
    }
    case "INTEGER": {
      const cleanData = rawColumnData.slice(1).filter((x) => !!x);
      const sum = cleanData.reduce((a, b) => a + parseInt(b), 0);
      const avg = sum / cleanData.length - 1;

      return (
        <Stat>
          <Name>{colName}</Name>
          <Value>
            <Strong>AVG</Strong> {avg}
          </Value>
        </Stat>
      );
    }
    case "OTHER":
    case "STRING":
    case "ID":
      return null;
  }
};

export default ColumnStats;
