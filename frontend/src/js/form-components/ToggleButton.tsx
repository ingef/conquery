import React, { FC } from "react";
import styled from "@emotion/styled";

const Root = styled("p")`
  margin: 0;
`;

const Option = styled("span")<{ active?: boolean }>`
  font-size: ${({ theme }) => theme.font.xs};
  display: inline-block;
  padding: 2px 8px;
  cursor: pointer;
  transition: color ${({ theme }) => theme.transitionTime},
    background-color ${({ theme }) => theme.transitionTime};
  color: ${({ theme, active }) => (active ? theme.col.black : theme.col.gray)};
  border: 1px solid ${({ theme }) => theme.col.gray};
  background-color: ${({ theme, active }) =>
    active ? "white" : theme.col.grayLight};

  margin-left: -1px;

  &:first-of-type {
    margin-left: 0;
    border-top-left-radius: 2px;
    border-bottom-left-radius: 2px;
  }

  &:last-of-type {
    border-top-right-radius: 2px;
    border-bottom-right-radius: 2px;
  }

  &:hover {
    background-color: ${({ theme, active }) =>
      active ? "white" : theme.col.grayVeryLight};
  }
`;

interface OptionsT {
  label: string;
  value: string;
}

interface PropsT {
  options: OptionsT[];
  input: {
    value: any;
    onChange: (value: any) => void;
  };
}

const ToggleButton: FC<PropsT> = ({ options, input }) => {
  return (
    <Root>
      {options.map(({ value, label }, i) => (
        <Option
          key={i}
          active={input.value === value}
          onClick={() => {
            if (value !== input.value) input.onChange(value);
          }}
        >
          {label}
        </Option>
      ))}
    </Root>
  );
};

export default ToggleButton;
