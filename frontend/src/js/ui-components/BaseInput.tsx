import styled from "@emotion/styled";
import {
  faCheck,
  faExclamationTriangle,
  faTimes,
} from "@fortawesome/free-solid-svg-icons";
import { FocusEvent, KeyboardEvent, forwardRef, useCallback } from "react";
import { useTranslation } from "react-i18next";

import type { CurrencyConfigT } from "../api/types";
import IconButton from "../button/IconButton";
import { isEmpty } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";
import WithTooltip from "../tooltip/WithTooltip";

import CurrencyInput from "./CurrencyInput";

const Root = styled("div")`
  position: relative;
`;

const Input = styled("input")<{ large?: boolean; disabled?: boolean }>`
  outline: 0;
  width: 100%;
  opacity: ${({ disabled }) => (disabled ? 0.5 : 1)};

  border: 1px solid ${({ theme }) => theme.col.grayMediumLight};
  padding: ${({ large }) =>
    large ? "10px 30px 10px 14px" : "6px 30px 6px 10px"};
  font-size: ${({ theme, large }) => (large ? theme.font.lg : theme.font.sm)};
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const SignalIcon = styled(FaIcon)`
  position: absolute;
  top: 8px;
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
const AbsoluteWrap = styled("div")`
  position: absolute;
  top: 5px;
  right: 35px;
`;

const ClearZoneIconButton = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 10px;
  cursor: pointer;
  height: 100%;
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
  onKeyPress?: (e: KeyboardEvent<HTMLInputElement>) => void;
}

export interface Props {
  className?: string;
  inputType: string;
  money?: boolean;
  valid?: boolean;
  invalid?: boolean;
  invalidText?: string;
  placeholder?: string;
  value: string | number | null;
  large?: boolean;
  inputProps?: InputProps;
  currencyConfig?: CurrencyConfigT;
  disabled?: boolean;
  onFocus?: (e: FocusEvent<HTMLInputElement>) => void;
  onBlur?: (e: FocusEvent<HTMLInputElement>) => void;
  onClick?: (e: React.MouseEvent<HTMLInputElement>) => void;
  onChange: (val: string | number | null) => void;
}

const usePatternMatching = ({ pattern }: { pattern?: string }) => {
  const onKeyPress = useCallback(
    (event: KeyboardEvent<HTMLInputElement>) => {
      if (!pattern) return;

      const regex = new RegExp(pattern);
      const key = String.fromCharCode(
        !event.charCode ? event.which : event.charCode,
      );

      if (!regex.test(key)) {
        event.preventDefault();
        return false;
      }
    },
    [pattern],
  );

  return pattern ? { onKeyPress } : {};
};

const BaseInput = forwardRef<HTMLInputElement, Props>(
  (
    {
      className,
      inputProps = {},
      currencyConfig,
      money,
      value,
      onChange,
      onFocus,
      onBlur,
      onClick,
      placeholder,
      large,
      inputType,
      valid,
      invalid,
      invalidText,
      disabled,
    },
    ref,
  ) => {
    const { t } = useTranslation();

    const patternMatchingProps = usePatternMatching({
      pattern: inputProps.pattern,
    });

    function safeOnChange(val: string | number | null) {
      if (
        (typeof val === "string" && val.length === 0) ||
        (typeof val === "number" && isNaN(val))
      ) {
        onChange(null);
      } else {
        onChange(val);
      }
    }

    const isCurrencyInput = money && !!currencyConfig;

    return (
      <Root className={className}>
        {isCurrencyInput ? (
          <CurrencyInput
            currencyConfig={currencyConfig}
            placeholder={placeholder}
            large={large}
            value={value as number | null}
            onChange={safeOnChange}
          />
        ) : (
          <Input
            placeholder={placeholder}
            type={inputType}
            ref={ref}
            onChange={(e) => {
              let value: string | number | null = e.target.value;

              if (inputType === "number") {
                value = parseFloat(value);
              }

              safeOnChange(value);
            }}
            value={exists(value) ? value : ""}
            large={large}
            disabled={disabled}
            onFocus={onFocus}
            onBlur={onBlur}
            onClick={onClick}
            onWheel={
              (e) =>
                (
                  e.target as HTMLElement
                ).blur() /* to disable scrolling for number */
            }
            {...inputProps}
            {...patternMatchingProps}
          />
        )}
        {exists(value) && !isEmpty(value) && (
          <>
            {valid && !invalid && <GreenIcon icon={faCheck} large={large} />}
            {invalid && (
              <WithTooltip text={invalidText}>
                <AbsoluteWrap>
                  <RedIcon icon={faExclamationTriangle} large={large} />
                </AbsoluteWrap>
              </WithTooltip>
            )}
            <ClearZoneIconButton
              tiny
              icon={faTimes}
              tabIndex={-1}
              disabled={disabled}
              title={t("common.clearValue")}
              aria-label={t("common.clearValue")}
              onClick={() => onChange(null)}
            />
          </>
        )}
      </Root>
    );
  },
);

export default BaseInput;
