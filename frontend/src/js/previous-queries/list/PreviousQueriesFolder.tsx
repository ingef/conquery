import styled from "@emotion/styled";
import React, { FC } from "react";
import Highlighter from "react-highlight-words";

import { exists } from "../../common/helpers/exists";
import FaIcon from "../../icon/FaIcon";

const SxFaIcon = styled(FaIcon)`
  margin-right: 8px;
`;

const Folder = styled("div")<{ active?: boolean; special?: boolean }>`
  display: inline-flex;
  align-items: flex-start;
  padding: 2px 7px;
  border-radius: ${({ theme }) => theme.borderRadius};
  font-size: ${({ theme }) => theme.font.sm};
  cursor: pointer;
  font-style: ${({ special }) => (special ? "italic" : "inherit")};

  background-color: ${({ theme, active }) =>
    active ? theme.col.grayLight : "transparent"};
  &:hover {
    background-color: ${({ theme }) => theme.col.blueGrayVeryLight};
  }
`;

const Text = styled("div")`
  flex-shrink: 0;
  color: ${({ theme }) => theme.col.black};
`;

const ResultCount = styled("span")`
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  padding: 2px 0;
  margin-right: 5px;
  font-size: ${({ theme }) => theme.font.xs};
  border-radius: ${({ theme }) => theme.borderRadius};
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
`;

interface Props {
  folder: string;
  resultCount: number | null;
  resultWords: string[];
  className?: string;
  active?: boolean;
  special?: boolean;
  empty?: boolean;
  onClick: () => void;
}

const PreviousQueriesFolder: FC<Props> = ({
  className,
  resultCount,
  resultWords,
  folder,
  active,
  special,
  empty,
  onClick,
}) => {
  return (
    <Folder
      key={folder}
      active={active}
      special={special}
      onClick={onClick}
      className={className}
      title={folder}
    >
      <SxFaIcon icon="folder" regular={special} active />
      {exists(resultCount) && <ResultCount>{resultCount}</ResultCount>}
      <Text>
        {!empty && resultWords.length > 0 ? (
          <Highlighter
            autoEscape
            searchWords={resultWords}
            textToHighlight={folder}
          />
        ) : (
          folder
        )}
      </Text>
    </Folder>
  );
};
export default PreviousQueriesFolder;
