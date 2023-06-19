import styled from "@emotion/styled";
import { Fragment, memo } from "react";

import { EntityInfo } from "../api/types";

const Grid = styled("div")`
  display: inline-grid;
  grid-template-columns: 1fr auto;
  gap: 0 20px;
  place-items: center start;
`;
const Label = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
`;
const Value = styled("div")<{ blurred?: boolean }>`
  font-size: ${({ theme }) => theme.font.sm};
  font-weight: 400;
  ${({ blurred }) => blurred && "filter: blur(6px);"}
`;

const EntityInfos = ({
  infos,
  blurred,
}: {
  infos: EntityInfo[];
  blurred?: boolean;
}) => {
  return (
    <Grid>
      {infos.map((info) => (
        <Fragment key={info.label}>
          <Label>{info.label}</Label>
          <Value blurred={blurred}>{info.value}</Value>
        </Fragment>
      ))}
    </Grid>
  );
};

export default memo(EntityInfos);
