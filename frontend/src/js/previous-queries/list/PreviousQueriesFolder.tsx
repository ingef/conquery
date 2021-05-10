import styled from "@emotion/styled";
import React, { FC } from "react";

import FaIcon from "../../icon/FaIcon";

const Folder = styled("div")`
  display: inline-flex;
  align-items: flex-start;
  padding: 4px 7px;
  border-radius: ${({ theme }) => theme.borderRadius};
  font-size: ${({ theme }) => theme.font.sm};
  cursor: pointer;

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
  onClick: () => void;
}

const PreviousQueriesFolder: FC<Props> = ({ folder, onClick }) => {
  return (
    <Folder key={folder} onClick={onClick}>
      <FaIcon icon="folder" main />
      <Text>{folder}</Text>
    </Folder>
  );
};
export default PreviousQueriesFolder;
