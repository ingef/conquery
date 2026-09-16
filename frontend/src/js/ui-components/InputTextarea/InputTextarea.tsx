import { faTimes } from "@fortawesome/free-solid-svg-icons";
import type { DetailedHTMLProps, Ref, TextareaHTMLAttributes } from "react";
import { Button as RacButton } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Icon } from "../Icon";

import Labeled from "../Labeled";

const textarea = tv({
  base: [
    "outline-0",
    "w-full",
    "rounded",
    "border border-gray-400",
    "py-[6px] pr-[30px] pl-[10px]",
    "text-sm",
  ],
});

const clearButton = tv({
  base: [
    "absolute top-0 right-[10px]",
    "h-[30px] px-1",
    "flex items-center",
    "text-gray-800 hover:text-red",
    "cursor-pointer",
  ],
});

interface OtherProps {
  label: string;
  className?: string;
  fullWidth?: boolean;
  indexPrefix?: number;
  tooltip?: string;
  onChange: (value: string | null) => void;
}

type InputTextareaProps = DetailedHTMLProps<
  TextareaHTMLAttributes<HTMLTextAreaElement>,
  HTMLTextAreaElement
>;

export const InputTextarea = ({
  ref,
  label,
  className,
  indexPrefix,
  tooltip,
  onChange,
  ...props
}: InputTextareaProps & OtherProps & { ref?: Ref<HTMLTextAreaElement> }) => {
  const { t } = useTranslation();

  return (
    <Labeled
      label={label}
      indexPrefix={indexPrefix}
      className={className}
      fullWidth
      tooltip={tooltip}
    >
      <div className="relative">
        <textarea
          ref={ref}
          className={textarea()}
          {...props}
          onChange={({ target: { value } }) =>
            value.length === 0 ? onChange(null) : onChange(value)
          }
          value={props.value || ""}
        />
        {props.value && (
          <RacButton
            className={clearButton()}
            excludeFromTabOrder
            aria-label={t("common.clearValue")}
            onPress={() => onChange(null)}
          >
            <Icon icon={faTimes} />
          </RacButton>
        )}
      </div>
    </Labeled>
  );
};
