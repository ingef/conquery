// @flow

import * as React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import IconButton from "../button/IconButton";

import { isEmpty } from "../common/helpers";
import type { CurrencyConfigT } from "../api/types";

import { MONEY_RANGE } from "./filterTypes";
import CurrencyInput from "./CurrencyInput";

const Root = styled("div")`
  position: relative;
  display: inline-block;
`;

const Input = styled("input")`
  min-width: 170px;
  padding: 8px 30px 8px 10px;
  font-size: ${({ theme }) => theme.font.sm};
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const ClearZone = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 10px;
  cursor: pointer;
  height: 36px;
  display: flex;
  align-items: center;

  &:hover {
    color: ${({ theme }) => theme.col.red};
  }
`;

type InputPropsType = {
  pattern?: RegExp,
  step?: number,
  min?: string,
  max?: string
};

type PropsType = {
  className?: string,
  inputType: string,
  valueType?: string,
  placeholder?: string,
  value: ?(number | string),
  inputProps?: InputPropsType,
  currencyConfig?: CurrencyConfigT,
  onChange: (?(number | string)) => void
};

const BaseInput = (props: PropsType) => {
  const { pattern } = props.inputProps || {};

  const handleKeyPress = event => {
    if (!pattern) return;

    const regex = new RegExp(pattern);
    const key = String.fromCharCode(
      !event.charCode ? event.which : event.charCode
    );

    if (!regex.test(key)) {
      event.preventDefault();
      return false;
    }
  };

  return (
    <Root className={props.className}>
      {props.valueType === MONEY_RANGE && !!props.currencyConfig ? (
        <CurrencyInput
          currencyConfig={props.currencyConfig}
          placeholder={props.placeholder}
          value={props.value}
          onChange={props.onChange}
        />
      ) : (
        <Input
          placeholder={props.placeholder}
          type={props.inputType}
          onChange={e => props.onChange(e.target.value)}
          onKeyPress={e => handleKeyPress(e)}
          value={props.value || ""}
          {...props.inputProps}
        />
      )}
      {!isEmpty(props.value) && (
        <ClearZone
          tiny
          icon="times"
          tabIndex="-1"
          title={T.translate("common.clearValue")}
          aria-label={T.translate("common.clearValue")}
          onClick={() => props.onChange(null)}
        />
      )}
    </Root>
  );
};

export default BaseInput;
