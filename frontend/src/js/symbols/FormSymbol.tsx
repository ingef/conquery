import { ChartColumnIcon } from "lucide-react";

import { InABox } from "./InABox";

const FormSymbol = ({ className }: { className?: string }) => {
  return (
    <InABox className={className}>
      <ChartColumnIcon className="text-primary-500" />
    </InABox>
  );
};

export default FormSymbol;
