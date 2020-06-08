import React, { FC } from "react";
import { ColumnDescriptionType } from "./Preview";

interface Props {
  columnType: ColumnDescriptionType;
  rawColumnData: string[];
}

const ColumnStats: FC<Props> = ({ columnType, rawColumnData }) => {
  switch (columnType) {
    case "DATE": {
      return <div>DATE Stats</div>;
    }
    case "INTEGER": {
      const sum = rawColumnData.slice(1).reduce((a, b) => a + parseFloat(b), 0);
      const avg = sum / rawColumnData.length - 1;

      return <div>AVG: {avg}</div>;
    }
    case "OTHER":
    case "STRING":
    case "ID":
      return null;
  }
};

export default ColumnStats;
