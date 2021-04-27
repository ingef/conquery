import styled from "@emotion/styled";
import React, { FC } from "react";

import WithTooltip from "../tooltip/WithTooltip";

const Root = styled("div")`
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;

  > span {
    &:first-of-type {
      margin-left: 0;
      border-top-left-radius: 2px;
      border-bottom-left-radius: 2px;
    }

    &:last-of-type {
      border-top-right-radius: 2px;
      border-bottom-right-radius: 2px;
    }
  }
  > div {
    &:first-of-type {
      span {
        margin-left: 0;
        border-top-left-radius: 2px;
        border-bottom-left-radius: 2px;
      }
    }
    &:last-of-type {
      span {
        border-top-right-radius: 2px;
        border-bottom-right-radius: 2px;
      }
    }
  }
`;

const Option = styled("span")<{ active?: boolean }>`
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

  &:hover {
    background-color: ${({ theme, active }) =>
      active ? "white" : theme.col.bg};
  }
`;

interface OptionsT {
  label: string;
  value: string;
  description?: string;
}

interface PropsT {
  className?: string;
  options: OptionsT[];
  input: {
    value: any;
    onChange: (value: any) => void;
  };
}

const ToggleButton: FC<PropsT> = ({ options, input, className }) => {
  return (
    <Root className={className}>
      {options.map(({ value, label, description }) => (
        <WithTooltip key={value} text={description}>
          <Option
            active={input.value === value}
            onClick={() => {
              if (value !== input.value) input.onChange(value);
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
