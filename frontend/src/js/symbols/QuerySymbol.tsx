import FaIcon from "../icon/FaIcon";

import { InABox } from "./InABox";

const QuerySymbol = ({ className }: { className?: string }) => {
  return (
    <InABox className={className}>
      <FaIcon icon="question" active className={className} />
    </InABox>
  );
};
export default QuerySymbol;
