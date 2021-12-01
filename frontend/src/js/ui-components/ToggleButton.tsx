import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { FC } from "react";

import WithTooltip from "../tooltip/WithTooltip";

const Root = styled("div")`
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
`;

const Option = styled("span")<{
  active?: boolean;
  isFirst?: boolean;
  isLast?: boolean;
}>`
  font-size: ${({ theme }) => theme.font.xs};
  display: inline-block;
  padding: 4px 8px;
  cursor: pointer;
  transition: color ${({ theme }) => theme.transitionTime},
    background-color ${({ theme }) => theme.transitionTime};
  color: ${({ theme, active }) => (active ? theme.col.black : theme.col.gray)};
  border: 1px solid ${({ theme }) => theme.col.gray};
  background-color: ${({ theme, active }) =>
    active ? "white" : theme.col.grayVeryLight};

  margin-left: -1px;
  margin-bottom: 2px;

  &:hover {
    background-color: ${({ theme, active }) =>
      active ? "white" : theme.col.bg};
  }

  ${({ isFirst }) =>
    isFirst &&
    css`
      margin-left: 0;
      border-top-left-radius: 2px;
      border-bottom-left-radius: 2px;
    `}
  ${({ isLast }) =>
    isLast &&
    css`
      border-top-right-radius: 2px;
      border-bottom-right-radius: 2px;
    `}
`;

interface OptionsT {
  label: string;
  value: string;
  description?: string;
}

interface PropsT {
  className?: string;
  options: OptionsT[];
  value: string;
  onChange: (value: string) => void;
}

const ToggleButton: FC<PropsT> = ({
  options,
  value: inputValue,
  onChange,
  className,
}) => {
  return (
    <Root className={className}>
      {options.map(({ value, label, description }, i) => (
        <WithTooltip key={value} text={description}>
          <Option
            isFirst={i === 0}
            isLast={i === options.length - 1}
            active={inputValue === value}
            onClick={() => {
              if (value !== inputValue) onChange(value);
            }}
          >
            {label}
          </Option>
        </WithTooltip>
      ))}
    </Root>
  );
};

export default ToggleButton;
