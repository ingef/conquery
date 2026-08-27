import { faTimes } from "@fortawesome/free-solid-svg-icons";
import type { DetailedHTMLProps, Ref, TextareaHTMLAttributes } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import IconButton from "../../button/IconButton";
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

const clearZoneIconButton = tv({
  base: [
    "absolute top-0 right-[10px]",
    "h-[30px]",
    "flex items-center",
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
          <IconButton
            className={clearZoneIconButton()}
            tiny
            icon={faTimes}
            tabIndex={-1}
            title={t("common.clearValue")}
            aria-label={t("common.clearValue")}
            onClick={() => onChange(null)}
          />
        )}
      </div>
    </Labeled>
  );
};
