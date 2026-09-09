import { tv } from "tailwind-variants";

import WithTooltip from "./WithTooltip";

const root = tv({ base: ["m-0", "flex flex-wrap items-center"] });

const option = tv({
  base: [
    "inline-block",
    "px-2 py-1",
    "-ml-px mb-[2px]",
    "cursor-pointer",
    "border border-gray-500",
    "text-xs",
    "transition-[color,background-color] duration-100",
  ],
  variants: {
    active: {
      true: ["text-gray-800", "bg-white hover:bg-white"],
      false: ["text-gray-500", "bg-gray-50 hover:bg-bg-50"],
    },
    isFirst: { true: ["ml-0", "rounded-l-[2px]"] },
    isLast: { true: "rounded-r-[2px]" },
  },
});

interface OptionsT {
  label: string;
  value: string;
  description?: string;
}

const ToggleButton = ({
  options,
  value: inputValue,
  onChange,
  className,
}: {
  className?: string;
  options: OptionsT[];
  value: string;
  onChange: (value: string) => void;
}) => {
  return (
    <div className={root({ className })}>
      {options.map(({ value, label, description }, i) => (
        <WithTooltip key={value} text={description}>
          <button
            type="button"
            className={option({
              isFirst: i === 0,
              isLast: i === options.length - 1,
              active: inputValue === value,
            })}
            onClick={() => {
              if (value !== inputValue) onChange(value);
            }}
          >
            {label}
          </button>
        </WithTooltip>
      ))}
    </div>
  );
};

export default ToggleButton;
