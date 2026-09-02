import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

type PropsType = {
  className?: string;
  onToggle: (value: string) => void;
  activeValue: string;
  options: {
    label: string;
    value: string;
  }[];
};

const btn = tv({ base: "mx-auto my-0" });

const option = tv({
  base: [
    "block",
    "px-2 py-[2px]",
    "cursor-pointer",
    "text-xs",
    "transition-[color,background-color] duration-100",
    "border-x border-primary-200",
    // first child's left border does not overlap
    "first-of-type:ml-0",
    "first-of-type:border-t first-of-type:rounded-t-[2px]",
    "last-of-type:border-b last-of-type:rounded-b-[2px]",
  ],
  variants: {
    active: {
      true: ["text-gray-800", "bg-primary-50 hover:bg-primary-50"],
      false: ["text-gray-500", "bg-white hover:bg-gray-50"],
    },
  },
});

export const Option = ({
  className,
  active,
  ...props
}: ComponentProps<"span"> & { active?: boolean }) => (
  <span className={option({ active: !!active, className })} {...props} />
);

const VerticalToggleButton = (props: PropsType) => {
  return (
    <p className={btn({ className: props.className })}>
      {props.options.map(({ value, label }, i) => (
        <Option
          key={i}
          active={props.activeValue === value}
          onClick={() => {
            if (value !== props.activeValue) props.onToggle(value);
          }}
        >
          {label}
        </Option>
      ))}
    </p>
  );
};

export default VerticalToggleButton;
