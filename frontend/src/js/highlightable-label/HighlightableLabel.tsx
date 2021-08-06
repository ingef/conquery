import { css } from "@emotion/react";
import styled from "@emotion/styled";
import React, { FC } from "react";

interface PropsT {
  className?: string;
  isHighlighted?: boolean;
}

const Label = styled("span")<{ isHighlighted?: boolean }>`
  ${({ theme, isHighlighted }) =>
    isHighlighted &&
    css`
      background-color: ${theme.col.grayVeryLight};
      border-radius: ${theme.borderRadius};
      padding: 0 3px;
    `};
`;

const HighlightableLabel: FC<PropsT> = ({
  isHighlighted,
  className,
  children,
}) => {
  return (
    <Label className={className} isHighlighted={isHighlighted}>
      {children}
    </Label>
  );
};

export default HighlightableLabel;
