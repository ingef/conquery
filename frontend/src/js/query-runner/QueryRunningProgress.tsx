import { tv } from "tailwind-variants";

const progressText = tv({
  base: ["mr-[10px]", "text-xl", "font-bold", "text-primary-200"],
});

// progress is between 0 and 1

const QueryRunningProgress = ({ progress }: { progress: number }) => {
  return <div className={progressText()}>{Math.round(progress * 100)} %</div>;
};

export default QueryRunningProgress;
