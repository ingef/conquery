import { ChartColumnIcon } from "lucide-react";

import { Icon } from "../ui-components/Icon";

import { InABox } from "./InABox";

const FormSymbol = ({ className }: { className?: string }) => {
  return (
    <InABox className={className}>
      <Icon icon={ChartColumnIcon} className="text-primary-500" />
    </InABox>
  );
};

export default FormSymbol;
