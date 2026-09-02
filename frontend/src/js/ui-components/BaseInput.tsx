import {
  faCheck,
  faExclamationTriangle,
  faTimes,
} from "@fortawesome/free-solid-svg-icons";
import {
  type FocusEvent,
  type KeyboardEvent,
  type Ref,
  useCallback,
} from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { CurrencyConfigT } from "../api/types";
import IconButton from "../button/IconButton";
import { isEmpty } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";
import CurrencyInput from "./CurrencyInput";
import WithTooltip from "./WithTooltip";

const root = tv({ base: "relative" });

const input = tv({
  base: [
    "outline-0",
    "w-full",
    "border border-gray-400",
    "rounded",
    "py-[6px] pr-[30px] pl-[10px]",
    "text-sm",
    "font-normal",
  ],
  variants: {
    large: { true: "py-[10px] pr-[30px] pl-[14px] text-xl" },
    disabled: { true: "opacity-50" },
  },
});

const greenIcon = tv({
  base: ["absolute top-2 right-[35px]", "opacity-80", "text-green"],
});

const redIcon = tv({ base: ["opacity-80", "text-red"] });

const absoluteWrap = tv({ base: "absolute top-[5px] right-[35px]" });

const clearZoneIconButton = tv({
  base: [
    "absolute top-0 right-[10px]",
    "h-full",
    "flex items-center",
    "cursor-pointer",
    "hover:text-red",
  ],
});

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

const BaseInput = ({
  ref,
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
}: Props & { ref?: Ref<HTMLInputElement> }) => {
  const { t } = useTranslation();

  const patternMatchingProps = usePatternMatching({
    pattern: inputProps.pattern,
  });

  function safeOnChange(val: string | number | null) {
    if (
      (typeof val === "string" && val.length === 0) ||
      (typeof val === "number" && Number.isNaN(val))
    ) {
      onChange(null);
    } else {
      onChange(val);
    }
  }

  const isCurrencyInput = money && !!currencyConfig;

  return (
    <div className={root({ className })}>
      {isCurrencyInput ? (
        <CurrencyInput
          currencyConfig={currencyConfig}
          placeholder={placeholder}
          large={large}
          value={value as number | null}
          onChange={safeOnChange}
        />
      ) : (
        <input
          className={input({ large, disabled })}
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
          {valid && !invalid && (
            <FaIcon icon={faCheck} large={large} className={greenIcon()} />
          )}
          {invalid && (
            <WithTooltip text={invalidText}>
              <div className={absoluteWrap()}>
                <FaIcon
                  icon={faExclamationTriangle}
                  large={large}
                  className={redIcon()}
                />
              </div>
            </WithTooltip>
          )}
          <IconButton
            className={clearZoneIconButton()}
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
    </div>
  );
};

export default BaseInput;
