import { memo } from "react";
import { tv } from "tailwind-variants";

const bar = tv({
  base: [
    "h-[7px] w-full",
    "rounded",
    "bg-[#ccc]",
    "shadow-[0_0_3px_0_rgba(0,0,0,0.1)]",
  ],
});

const barProgress = tv({ base: ["h-full", "rounded", "bg-primary-500"] });

interface Props {
  className?: string;
  donePercent: number;
}

const ProgressBar = ({ className, donePercent }: Props) => {
  return (
    <div className={bar({ className })}>
      <div className={barProgress()} style={{ width: `${donePercent}%` }} />
    </div>
  );
};

export default memo(ProgressBar);
