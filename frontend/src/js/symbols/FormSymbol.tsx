import { faChartColumn } from "@fortawesome/free-solid-svg-icons";

import FaIcon from "../icon/FaIcon";

import { InABox } from "./InABox";

const FormSymbol = ({ className }: { className?: string }) => {
  return (
    <InABox className={className}>
      <FaIcon icon={faChartColumn} active />
    </InABox>
  );
};

export default FormSymbol;
