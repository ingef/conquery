import { faChartColumn } from "@fortawesome/free-solid-svg-icons";

import { Icon } from "../ui-components/Icon";

import { InABox } from "./InABox";

const FormSymbol = ({ className }: { className?: string }) => {
  return (
    <InABox className={className}>
      <Icon icon={faChartColumn} className="text-primary-500" />
    </InABox>
  );
};

export default FormSymbol;
