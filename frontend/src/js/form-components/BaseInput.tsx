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

const Input = styled("input")<{ large?: boolean }>`
  min-width: 170px;
  padding: ${({ large }) =>
    large ? "10px 30px 10px 14px" : "8px 30px 8px 10px"};
  font-size: ${({ theme, large }) => (large ? theme.font.lg : theme.font.sm)};
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const ClearZone = styled(IconButton)`
  position: absolute;
  top: ${({ large }) => (large ? "5px" : "0")};
  right: 10px;
  cursor: pointer;
  height: 34px;
  display: flex;
  align-items: center;

  &:hover {
    color: ${({ theme }) => theme.col.red};
  }
`;

type InputPropsType = {
  pattern?: string;
  step?: number;
  min?: number;
  max?: number;
};

type PropsType = {
  className?: string;
  inputType: string;
  valueType?: string;
  placeholder?: string;
  value: number | string | null;
  large?: boolean;
  inputProps?: InputPropsType;
  currencyConfig?: CurrencyConfigT;
  onChange: (val: null | number | string) => void;
};

const BaseInput = (props: PropsType) => {
  const inputProps = props.inputProps || {};
  const { pattern } = props.inputProps || {};

  const handleKeyPress = (event) => {
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

  function safeOnChange(val: string | number | null) {
    if (typeof val === "string" && val.length === 0) {
      props.onChange(null);
    } else {
      props.onChange(val);
    }
  }

  return (
    <Root className={props.className}>
      {props.valueType === MONEY_RANGE && !!props.currencyConfig ? (
        <CurrencyInput
          currencyConfig={props.currencyConfig}
          placeholder={props.placeholder}
          value={props.value}
          onChange={safeOnChange}
        />
      ) : (
        <Input
          placeholder={props.placeholder}
          type={props.inputType}
          onChange={(e) => safeOnChange(e.target.value)}
          onKeyPress={(e) => handleKeyPress(e)}
          value={props.value || ""}
          large={props.large}
          {...inputProps}
        />
      )}
      {!isEmpty(props.value) && (
        <ClearZone
          tiny
          icon="times"
          tabIndex="-1"
          large={props.large}
          title={T.translate("common.clearValue")}
          ariaLabel={T.translate("common.clearValue")}
          onClick={() => props.onChange(null)}
        />
      )}
    </Root>
  );
};

export default BaseInput;
