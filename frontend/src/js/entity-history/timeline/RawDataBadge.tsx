import styled from "@emotion/styled";

import { ColumnDescription } from "../../api/types";
import { EntityEvent } from "../reducer";

const Badge = styled("div")`
  border-radius: ${({ theme }) => theme.borderRadius};
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  padding: 1px 4px;
  font-size: ${({ theme }) => theme.font.xs};
  color: white;
  font-weight: 700;
`;

interface Props {
  event: EntityEvent;
  className?: string;
  sourceColumn: ColumnDescription;
}

export const RawDataBadge = ({ className, event, sourceColumn }: Props) => {
  return (
    <Badge
      className={className}
      onClick={() => {
        if (navigator.clipboard) {
          navigator.clipboard.writeText(JSON.stringify(event, null, 2));
        }
      }}
    >
      {event[sourceColumn.label]}
    </Badge>
  );
};
