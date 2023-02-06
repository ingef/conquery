import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { forwardRef, ReactNode } from "react";

import { IndexPrefix } from "../common/components/IndexPrefix";
import { exists } from "../common/helpers/exists";
import InfoTooltip from "../tooltip/InfoTooltip";

import Label from "./Label";
import Optional from "./Optional";

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
  optional?: boolean;
  tooltip?: string;
  htmlFor?: string;
}

const Labeled = forwardRef<HTMLLabelElement, Props>(
  (
    {
      indexPrefix,
      className,
      fullWidth,
      label,
      tinyLabel,
      largeLabel,
      tooltip,
      optional,
      htmlFor,
      children,
    },
    ref,
  ) => {
    return (
      <Root
        ref={ref}
        className={className}
        fullWidth={fullWidth}
        htmlFor={htmlFor}
      >
        <Label fullWidth={fullWidth} tiny={tinyLabel} large={largeLabel}>
          {exists(indexPrefix) && <IndexPrefix># {indexPrefix}</IndexPrefix>}
          {optional && <Optional />}
          {label}
          {exists(tooltip) && <InfoTooltip text={tooltip} />}
        </Label>
        {children}
      </Root>
    );
  },
);

export default Labeled;
