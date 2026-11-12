import { tv } from "tailwind-variants";

import { useAppTheme } from "../app-theme-context";

const spinner = tv({
  base: ["h-[30px] w-[30px]", "bg-no-repeat", "bg-size-[30px]", "animate-spin"],
});

export const QueryRunningSpinner = ({ className }: { className?: string }) => {
  const { img } = useAppTheme();

  return (
    <div
      className={spinner({ className })}
      style={{ backgroundImage: `url("${img.spinner}")` }}
    />
  );
};
