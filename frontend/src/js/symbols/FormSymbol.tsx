import FaIcon from "../icon/FaIcon";

import { InABox } from "./InABox";

const FormSymbol = ({ className }: { className?: string }) => {
  return (
    <InABox className={className}>
      <FaIcon icon="chart-column" active />
    </InABox>
  );
};

export default FormSymbol;
