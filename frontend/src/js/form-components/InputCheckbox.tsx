import React from "react";
import styled from "@emotion/styled";

import type { FieldPropsType } from "redux-form";

const Row = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
  cursor: pointer;
`;

const Label = styled("span")`
  margin-left: 10px;
  font-size: ${({ theme }) => theme.font.sm};
  line-height: 1;
`;

const Container = styled("div")`
  flex-shrink: 0;
  position: relative;
  font-size: 22px;
  width: 20px;
  height: 20px;
  border: 2px solid ${({ theme }) => theme.col.blueGrayDark};
  border-radius: ${({ theme }) => theme.borderRadius};
  box-sizing: content-box;
`;

const Checkmark = styled("div")`
  position: absolute;
  top: 0;
  left: 0;
  height: 20px;
  width: 20px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};

  &:after {
    content: "";
    position: absolute;
    left: 6px;
    top: 2px;
    width: 5px;
    height: 10px;
    border: solid white;
    border-width: 0 3px 3px 0;
    transform: rotate(45deg);
  }
`;

type PropsType = FieldPropsType & {
  label: string;
  className?: string;
};

const InputCheckbox = (props: PropsType) => (
  <Row
    className={props.className}
    onClick={() => props.input.onChange(!props.input.value)}
  >
    <Container>{!!props.input.value && <Checkmark />}</Container>
    <Label>{props.label}</Label>
  </Row>
);

export default InputCheckbox;
