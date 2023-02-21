import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { ReactNode } from "react";

const Label = styled("span")<{ isHighlighted?: boolean }>`
  ${({ theme, isHighlighted }) =>
    isHighlighted &&
    css`
      background-color: ${theme.col.grayVeryLight};
      border-radius: ${theme.borderRadius};
      padding: 0 3px;
    `};
`;

const HighlightableLabel = ({
  isHighlighted,
  className,
  children,
}: {
  children: ReactNode;
  className?: string;
  isHighlighted?: boolean;
}) => {
  return (
    <Label className={className} isHighlighted={isHighlighted}>
      {children}
    </Label>
  );
};

export default HighlightableLabel;
