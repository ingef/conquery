import styled from "@emotion/styled";
import React, { FC } from "react";

import FaIcon from "../../icon/FaIcon";

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
  margin-left: 10px;
`;

interface Props {
  folder: string;
  className?: string;
  active?: boolean;
  empty?: boolean;
  onClick: () => void;
}

const PreviousQueriesFolder: FC<Props> = ({
  className,
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
    >
      <FaIcon icon="folder" regular={empty} main />
      <Text>{folder}</Text>
    </Folder>
  );
};
export default PreviousQueriesFolder;
