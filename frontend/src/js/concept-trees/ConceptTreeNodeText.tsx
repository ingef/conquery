import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { forwardRef } from "react";
import Highlighter from "react-highlight-words";

import FaIcon from "../icon/FaIcon";

// Root with transparent background
const Root = styled("div")<{ depth: number }>`
  position: relative; // Needed to fix a drag & drop issue in Safari
  cursor: pointer;
  padding: 0 15px 0 15px;
  margin: 2px 0;
  padding-left: ${({ depth }) => depth * 15 + "px"};
  display: flex;
`;

const Text = styled("p")<{
  red?: boolean;
  disabled?: boolean;
  isOpen?: boolean;
}>`
  user-select: none;
  border-radius: ${({ theme }) => theme.borderRadius};
  margin: 0;
  padding: 0 10px;
  line-height: 18px;
  color: ${({ theme, red, disabled }) =>
    red ? theme.col.red : disabled ? theme.col.gray : theme.col.black};
  display: inline-flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;

  border: 1px solid transparent;
  background-color: ${({ theme, isOpen }) =>
    isOpen ? theme.col.grayVeryLight : theme.col.bg};

  ${({ theme, disabled }) =>
    !disabled &&
    css`
      &:hover {
        border-color: ${theme.col.blueGray};
      }
    `};
`;

const noShrink = css`
  display: inline-block;
  flex-shrink: 0;
`;

const DashIconContainer = styled("span")`
  flex-shrink: 0;
  width: 34px;
  text-align: left;
  padding-left: 14px;
  ${noShrink};
`;

const FolderIconContainer = styled("span")`
  width: 20px;
  ${noShrink};
`;

const CaretIconContainer = styled("span")`
  width: 14px;
  ${noShrink};
`;

const Description = styled("span")`
  padding-left: 3px;
  ${noShrink};
`;

const ResultsNumber = styled("span")`
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  padding: 2px 4px;
  margin-right: 5px;
  font-size: ${({ theme }) => theme.font.xs};
  border-radius: ${({ theme }) => theme.borderRadius};
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
`;

interface PropsT {
  label: string;
  depth: number;
  className?: string;
  description?: string;
  resultCount?: number | null;
  searchWords?: string[] | null;
  isOpen?: boolean;
  isStructFolder?: boolean;
  hasChildren?: boolean;
  red?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}

const ConceptTreeNodeText = forwardRef<HTMLDivElement, PropsT>(
  (
    {
      label,
      description,
      resultCount,
      searchWords,
      className,
      depth,

      isOpen,
      isStructFolder,
      red,
      disabled,
      hasChildren,

      onClick,
    },
    ref,
  ) => (
    <Root ref={ref} className={className} depth={depth}>
      <Text onClick={onClick} isOpen={isOpen} red={red} disabled={disabled}>
        {hasChildren && (
          <>
            <CaretIconContainer>
              <FaIcon
                disabled={disabled}
                active
                icon={!!isOpen ? "caret-down" : "caret-right"}
              />
            </CaretIconContainer>
            <FolderIconContainer>
              <FaIcon
                active
                disabled={disabled}
                regular={!!isStructFolder}
                icon={!!isOpen ? "folder-open" : "folder"}
              />
            </FolderIconContainer>
          </>
        )}
        {!hasChildren && (
          <DashIconContainer>
            <FaIcon
              disabled={disabled}
              large
              active
              icon={disabled ? "ellipsis-h" : "minus"}
            />
          </DashIconContainer>
        )}
        {resultCount && <ResultsNumber>{resultCount}</ResultsNumber>}
        <span>
          {!!searchWords ? (
            <Highlighter
              searchWords={searchWords}
              autoEscape={true}
              textToHighlight={label}
            />
          ) : (
            label
          )}
        </span>
        {!!description && (
          <Description>
            {!!searchWords ? (
              <Highlighter
                searchWords={searchWords}
                autoEscape={true}
                textToHighlight={description}
              />
            ) : (
              `- ${description}`
            )}
          </Description>
        )}
      </Text>
    </Root>
  ),
);

export default ConceptTreeNodeText;
