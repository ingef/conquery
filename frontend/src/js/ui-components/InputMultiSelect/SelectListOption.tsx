import { type ComponentProps, memo, type Ref } from "react";
import ReactMarkdown from "react-markdown";
import { tv } from "tailwind-variants";

import type { SelectOptionT } from "../../api/types";

const container = tv({
  base: [
    "px-2 py-[3px]",
    "cursor-pointer",
    "text-gray-800",
    "text-base",
    "font-light",
    "transition-[background-color] duration-100",
    // to style react-markdown
    "[&_p]:m-0",
  ],
  variants: {
    active: { true: "bg-primary-50" },
    disabled: { true: "cursor-not-allowed opacity-40" },
  },
});

interface StyleProps {
  active?: boolean;
  disabled?: boolean;
}

interface Props extends StyleProps, Omit<ComponentProps<"div">, "ref"> {
  option: SelectOptionT;
}

const SelectListOption = ({
  ref,
  option,
  active,
  disabled: _disabled, // the option decides, see below
  className,
  ...props
}: Props & { ref?: Ref<HTMLDivElement> }) => {
  const label = option.label || String(option.value);

  return (
    <div
      {...props}
      className={container({ active, disabled: option.disabled, className })}
      ref={ref}
    >
      {option.displayLabel ? (
        option.displayLabel
      ) : (
        <ReactMarkdown>{label}</ReactMarkdown>
      )}
    </div>
  );
};

export default memo(SelectListOption);
