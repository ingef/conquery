import { faDiagramProject } from "@fortawesome/free-solid-svg-icons";

import FaIcon from "../icon/FaIcon";

import { InABox } from "./InABox";

const QuerySymbol = ({ className }: { className?: string }) => {
  return (
    <InABox className={className}>
      <FaIcon icon={faDiagramProject} active className={className} />
    </InABox>
  );
};
export default QuerySymbol;
