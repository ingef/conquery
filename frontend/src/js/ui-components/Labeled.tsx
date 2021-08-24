import { css } from "@emotion/react";
import styled from "@emotion/styled";
import React, { ReactNode } from "react";

import { IndexPrefix } from "../common/components/IndexPrefix";
import { exists } from "../common/helpers/exists";

import Label from "./Label";

const Root = styled("label")<{ fullWidth?: boolean }>`
  ${({ fullWidth }) =>
    fullWidth &&
    css`
      width: 100%;
      input {
        width: 100%;
      }
    `};
`;

interface Props {
  label: ReactNode;
  indexPrefix?: number;
  className?: string;
  tinyLabel?: boolean;
  largeLabel?: boolean;
  fullWidth?: boolean;
  children?: React.ReactNode;
}

const Labeled = ({
  indexPrefix,
  className,
  fullWidth,
  label,
  tinyLabel,
  largeLabel,
  children,
}: Props) => {
  return (
    <Root className={className} fullWidth={fullWidth}>
      <Label fullWidth={fullWidth} tiny={tinyLabel} large={largeLabel}>
        {exists(indexPrefix) && <IndexPrefix># {indexPrefix}</IndexPrefix>}
        {label}
      </Label>
      {children}
    </Root>
  );
};

export default Labeled;
