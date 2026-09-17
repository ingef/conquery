import { WorkflowIcon } from "lucide-react";
import { tv } from "tailwind-variants";

import { InABox } from "./InABox";

const icon = tv({ base: "text-primary-500" });

const QuerySymbol = ({ className }: { className?: string }) => {
  return (
    <InABox className={className}>
      <WorkflowIcon className={icon({ className })} />
    </InABox>
  );
};
export default QuerySymbol;
