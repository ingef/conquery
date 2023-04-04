import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { faFolder } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";

import { IndexPrefix } from "../common/components/IndexPrefix";
import { exists } from "../common/helpers/exists";
import { Icon } from "../icon/FaIcon";

const Container = styled("div")`
  display: grid;
  grid-template-columns: auto 2fr 30px 3fr;
  align-items: flex-start;
  gap: 0 8px;
  padding: 3px 0;
  font-size: ${({ theme }) => theme.font.sm};
`;

const Text = styled("span")<{ bold?: boolean }>`
  margin: 0;
  color: ${({ theme }) => theme.col.gray};
  ${({ bold, theme }) =>
    bold &&
    css`
      color: ${theme.col.black};
      font-weight: 400;
    `};
`;

const SxIndexPrefix = styled(IndexPrefix)`
  margin-right: 0;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  color: white;
  font-weight: 700;
`;
const Right = styled("div")`
  display: flex;
  justify-content: flex-end;
  align-items: center;
`;

export const DropdownOption = memo(
  ({
    conceptLabel,
    filterLabel,
    filterIdx,
  }: {
    conceptLabel: string;
    filterLabel?: string;
    filterIdx?: number;
  }) => {
    const hasDifferentFilterLabel = exists(filterLabel) && exists(filterIdx);

    return (
      <Container>
        <Icon
          icon={faFolder}
          active={!hasDifferentFilterLabel}
          gray={hasDifferentFilterLabel}
        />
        <Text bold={!hasDifferentFilterLabel}>{conceptLabel}</Text>
        {hasDifferentFilterLabel && (
          <>
            <Right>
              <SxIndexPrefix># {filterIdx}</SxIndexPrefix>
            </Right>
            <Text bold>{filterLabel}</Text>
          </>
        )}
      </Container>
    );
  },
);
