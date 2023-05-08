import styled from "@emotion/styled";

import { DateRangeT } from "../../api/types";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  font-family: monospace;
  display: inline-grid;
  gap: 0 5px;
  grid-template-columns: auto 1fr;
`;

const Label = styled("div")`
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
  justify-self: flex-end;
`;

export const DateRange = ({ dateRange }: { dateRange: DateRangeT }) => {
  return (
    <Root>
      {dateRange.min && (
        <>
          <Label>from</Label>
          <span>{dateRange.min}</span>
        </>
      )}
      {dateRange.max && dateRange.max !== dateRange.min && (
        <>
          <Label>to</Label>
          <span>{dateRange.max}</span>
        </>
      )}
    </Root>
  );
};
