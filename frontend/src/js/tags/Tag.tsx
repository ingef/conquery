import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { FC } from "react";

const Root = styled("p")<{ isClickable?: boolean; isSelected?: boolean }>`
  display: inline-block;
  padding: 4px 4px;
  margin: 0 3px 3px 0;
  font-size: ${({ theme }) => theme.font.xs};
  line-height: ${({ theme }) => theme.font.xs};
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayMediumLight};
  white-space: nowrap;

  ${({ isClickable, theme, isSelected }) =>
    isClickable &&
    css`
      cursor: pointer;

      ${!isSelected &&
      css`
        &:hover {
          border-color: ${theme.col.gray};
        }
      `}
    `};

  ${({ isSelected, theme }) =>
    isSelected &&
    css`
      background-color: ${theme.col.blueGrayLight};
      color: white;
      font-weight: 700;
      border-color: ${theme.col.blueGrayLight};
    `};
`;

interface Props {
  className?: string;
  isSelected: boolean;
  onClick: () => void;
}

const Tag: FC<Props> = ({ children, className, onClick, isSelected }) => {
  return (
    <Root
      className={className}
      isClickable={!!onClick}
      isSelected={!!isSelected}
      onClick={onClick}
    >
      {children}
    </Root>
  );
};

export default Tag;
