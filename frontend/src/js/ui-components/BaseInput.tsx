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
import { Button as RacButton } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { CurrencyConfigT } from "../api/types";
import { isEmpty } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";
import CurrencyInput from "./CurrencyInput";
import { Icon } from "./Icon";
import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
  tooltipDelay,
} from "./Tooltip";

const root = tv({ base: "relative" });

const input = tv({
  base: [
    "outline-0",
    "w-full",
    "border border-gray-400",
    "rounded",
    "h-[30px] pr-[30px] pl-[10px]",
    "text-sm",
    "font-normal",
  ],
  variants: {
    large: { true: "h-9 pr-[30px] pl-[14px] text-base" },
    disabled: { true: "opacity-50" },
  },
});

const greenIcon = tv({
  base: ["absolute top-2 right-[35px]", "opacity-80", "text-green"],
});

const redIcon = tv({ base: ["opacity-80", "text-red"] });

const absoluteWrap = tv({ base: "absolute top-[5px] right-[35px]" });

const clearButton = tv({
  base: [
    "absolute top-0 right-[10px]",
    "h-full px-1",
    "flex items-center",
    "text-gray-800 hover:text-red",
    "cursor-pointer",
    "disabled:cursor-not-allowed disabled:opacity-40",
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
          {valid && !invalid && <Icon icon={faCheck} className={greenIcon()} />}
          {invalid && (
            <TooltipTrigger delay={tooltipDelay.immediate}>
              <TooltipTarget
                as="div"
                role="img"
                aria-label={invalidText}
                excludeFromTabOrder
                className={absoluteWrap()}
              >
                <Icon icon={faExclamationTriangle} className={redIcon()} />
              </TooltipTarget>
              <Tooltip>{invalidText}</Tooltip>
            </TooltipTrigger>
          )}
          <RacButton
            className={clearButton()}
            excludeFromTabOrder
            isDisabled={disabled}
            aria-label={t("common.clearValue")}
            onPress={() => onChange(null)}
          >
            <Icon icon={faTimes} />
          </RacButton>
        </>
      )}
    </div>
  );
};

export default BaseInput;
