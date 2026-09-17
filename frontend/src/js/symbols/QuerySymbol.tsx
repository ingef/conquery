import { WorkflowIcon } from "lucide-react";

import { Icon } from "../ui-components/Icon";

import { InABox } from "./InABox";

const QuerySymbol = ({ className }: { className?: string }) => {
  return (
    <InABox className={className}>
      <Icon icon={WorkflowIcon} className={[className, "text-primary-500"]} />
    </InABox>
  );
};
export default QuerySymbol;
