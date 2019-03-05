// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import NumberFormat from "react-number-format";
import { Decimal } from "decimal.js";

import IconButton from "../button/IconButton";

import { isEmpty } from "../common/helpers";
import { MONEY_RANGE } from "./filterTypes";

const Root = styled("div")`
  position: relative;
`;

const Input = styled("input")`
  min-width: 170px;
  padding-right: 30px;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 8px 10px;
  border-radius: 3px;
`;

const ClearZone = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 5px;
  cursor: pointer;
  height: 36px;
  display: flex;
  align-items: center;

  &:hover {
    color: ${({ theme }) => theme.col.red};
  }
`;

type PropsType = {
  className?: string,
  inputType: string,
  valueType?: string,
  placeholder?: string,
  value: ?(number | string),
  formattedValue?: string,
  inputProps?: Object,
  onChange: Function
};

type NumberFormatValueType = {
  floatValue: number,
  formattedValue: string,
  value: string
};

const ClearableInput = (props: PropsType) => {
  const { currency, pattern } = props.inputProps || {};

  const handleKeyPress = event => {
    if (!pattern) return;

    var regex = new RegExp(pattern);
    var key = String.fromCharCode(
      !event.charCode ? event.which : event.charCode
    );
    if (!regex.test(key)) {
      event.preventDefault();
      return false;
    }
  };

  return (
    <Root className={props.className}>
      {props.valueType === MONEY_RANGE ? (
        <NumberFormat
          prefix={currency.prefix || ""}
          thousandSeparator={currency.thousandSeparator || ""}
          decimalSeparator={currency.decimalSeparator || ""}
          decimalScale={currency.decimalScale || ""}
          className="clearable-input__input"
          placeholder={props.placeholder}
          type={props.inputType}
          onValueChange={(values: NumberFormatValueType) => {
            const { formattedValue, floatValue } = values;
            const parsed = new Decimal(floatValue).mul(currency.factor || 0);

            props.onChange(parsed, formattedValue);
          }}
          value={props.formattedValue}
          {...props.inputProps}
        />
      ) : (
        <Input
          placeholder={props.placeholder}
          type={props.inputType}
          onChange={e => props.onChange(e.target.value)}
          onKeyPress={e => handleKeyPress(e)}
          value={props.value}
          {...props.inputProps}
        />
      )}
      {!isEmpty(props.value) && (
        <ClearZone
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

export default ClearableInput;
