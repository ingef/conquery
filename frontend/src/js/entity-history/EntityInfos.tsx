import styled from "@emotion/styled";
import { Fragment, memo } from "react";

import { EntityInfo } from "../api/types";

const Grid = styled("div")`
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0 10px;
`;
const Label = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  font-weight: 400;
`;
const Value = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
`;

const EntityInfos = ({ infos }: { infos: EntityInfo[] }) => {
  return (
    <Grid>
      {infos.map((info) => (
        <Fragment key={info.label}>
          <Label>{info.label}</Label>
          <Value>{info.value}</Value>
        </Fragment>
      ))}
    </Grid>
  );
};

export default memo(EntityInfos);
