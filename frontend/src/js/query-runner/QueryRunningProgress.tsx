import { tv } from "tailwind-variants";

import { headingStyle } from "../ui-components/Typography";

const progressText = tv({
  base: [headingStyle({ level: 2 }), "text-primary-200"],
});

// progress is between 0 and 1

const QueryRunningProgress = ({ progress }: { progress: number }) => {
  return <div className={progressText()}>{Math.round(progress * 100)} %</div>;
};

export default QueryRunningProgress;
