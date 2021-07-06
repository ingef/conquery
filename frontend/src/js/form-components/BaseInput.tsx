import styled from "@emotion/styled";
import * as React from "react";
import { useTranslation } from "react-i18next";

import type { CurrencyConfigT } from "../api/types";
import IconButton from "../button/IconButton";
import { isEmpty } from "../common/helpers";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";
import WithTooltip from "../tooltip/WithTooltip";

import CurrencyInput from "./CurrencyInput";

const Root = styled("div")`
  position: relative;
  display: inline-block;
`;

const Input = styled("input")<{
  large?: boolean;
  valid?: boolean;
  invalid?: boolean;
}>`
  outline: 0;
  border: 1px solid ${({ theme }) => theme.col.grayMediumLight};
  font-size: ${({ theme }) => theme.font.md};
  min-width: 170px;

  padding: ${({ large }) =>
    large ? "10px 30px 10px 14px" : "8px 30px 8px 10px"};
  font-size: ${({ theme, large }) => (large ? theme.font.lg : theme.font.sm)};
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const SignalIcon = styled(FaIcon)`
  position: absolute;
  top: ${({ large }) => (large ? "14px" : "10px")};
  right: 35px;
  opacity: 0.8;
`;

const GreenIcon = styled(SignalIcon)`
  color: ${({ theme }) => theme.col.green};
`;
const RedIcon = styled(FaIcon)`
  color: ${({ theme }) => theme.col.red};
  opacity: 0.8;
`;

const SxWithTooltip = styled(WithTooltip)`
  position: absolute;
  top: 7px;
  right: 35px;
`;

const ClearZoneIconButton = styled(IconButton)`
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

interface InputProps {
  autoFocus?: boolean;
  pattern?: string;
  step?: number;
  min?: number;
  max?: number;
  onKeyPress?: (e: React.KeyboardEvent<HTMLInputElement>) => void;
}

interface Props {
  className?: string;
  inputType: string;
  money?: boolean;
  valid?: boolean;
  invalid?: boolean;
  placeholder?: string;
  value: number | string | null;
  large?: boolean;
  inputProps?: InputProps;
  currencyConfig?: CurrencyConfigT;
  onChange: (val: null | number | string) => void;
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void;
}

const BaseInput = (props: Props) => {
  const { t } = useTranslation();
  const inputProps = props.inputProps || {};
  const { pattern } = props.inputProps || {};

  const handleKeyPress = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (!pattern) return;

    const regex = new RegExp(pattern);
    const key = String.fromCharCode(
      !event.charCode ? event.which : event.charCode,
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

  const isCurrencyInput = props.money && !!props.currencyConfig;

  return (
    <Root className={props.className}>
      {isCurrencyInput ? (
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
          onChange={(e) => {
            let value: string | number | null = e.target.value;

            if (props.inputType === "number") {
              value = parseFloat(value);
            }

            safeOnChange(value);
          }}
          onKeyPress={(e) => handleKeyPress(e)}
          value={exists(props.value) ? props.value : ""}
          large={props.large}
          valid={props.valid}
          invalid={props.invalid}
          onBlur={props.onBlur}
          {...inputProps}
        />
      )}
      {exists(props.value) && !isEmpty(props.value) && (
        <>
          {props.valid && !props.invalid && (
            <GreenIcon icon="check" large={props.large} />
          )}
          {props.invalid && (
            <SxWithTooltip text={t("common.dateInvalid")}>
              <RedIcon icon="exclamation-triangle" large={props.large} />
            </SxWithTooltip>
          )}
          <ClearZoneIconButton
            tiny
            icon="times"
            tabIndex={-1}
            large={props.large}
            title={t("common.clearValue")}
            aria-label={t("common.clearValue")}
            onClick={() => props.onChange(null)}
          />
        </>
      )}
    </Root>
  );
};

export default BaseInput;
