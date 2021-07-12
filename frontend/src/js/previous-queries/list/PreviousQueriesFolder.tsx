import styled from "@emotion/styled";
import React, { FC } from "react";

import { exists } from "../../common/helpers/exists";
import FaIcon from "../../icon/FaIcon";

const SxFaIcon = styled(FaIcon)`
  margin-right: 10px;
`;

const Folder = styled("div")<{ active?: boolean; empty?: boolean }>`
  display: inline-flex;
  align-items: flex-start;
  padding: 2px 7px;
  border-radius: ${({ theme }) => theme.borderRadius};
  font-size: ${({ theme }) => theme.font.sm};
  cursor: pointer;
  font-style: ${({ empty }) => (empty ? "italic" : "inherit")};

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
  padding: 2px 4px;
  margin-right: 5px;
  font-size: ${({ theme }) => theme.font.xs};
  border-radius: ${({ theme }) => theme.borderRadius};
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
`;

interface Props {
  folder: string;
  resultCount: number | null;
  className?: string;
  active?: boolean;
  empty?: boolean;
  onClick: () => void;
}

const PreviousQueriesFolder: FC<Props> = ({
  className,
  resultCount,
  folder,
  active,
  empty,
  onClick,
}) => {
  return (
    <Folder
      key={folder}
      active={active}
      empty={empty}
      onClick={onClick}
      className={className}
      title={folder}
    >
      <SxFaIcon icon="folder" regular={empty} active />
      {exists(resultCount) && <ResultCount>{resultCount}</ResultCount>}
      <Text>{folder}</Text>
    </Folder>
  );
};
export default PreviousQueriesFolder;
