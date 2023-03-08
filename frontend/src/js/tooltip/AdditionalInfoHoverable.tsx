import styled from "@emotion/styled";
import { ReactNode } from "react";
import { useDispatch } from "react-redux";

import type { ConceptT } from "../api/types";
import { isEmpty } from "../common/helpers/commonHelper";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";

import { toggleAdditionalInfos, displayAdditionalInfos } from "./actions";
import { AdditionalInfosType } from "./reducer";

const Root = styled("div")`
  cursor: pointer;
`;

// Allowlist the data we pass (especially: don't pass all children)
const getAdditionalInfos = (
  node: ConceptT,
  parent?: string,
): AdditionalInfosType => ({
  label: node.label,
  description: node.description,
  isFolder: !!node.children && node.children.length > 0,
  matchingEntries: node.matchingEntries,
  matchingEntities: node.matchingEntities,
  dateRange: node.dateRange,
  infos: node.additionalInfos,
  parent: parent ? getConceptById(parent)?.label : null,
});

const AdditionalInfoHoverable = ({
  node,
  className,
  children,
  parent,
}: {
  children: ReactNode;
  node: ConceptT;
  className?: string;
  parent?: string;
}) => {
  const dispatch = useDispatch();

  const onDisplayAdditionalInfos = () => {
    if (!node.additionalInfos && isEmpty(node.matchingEntries)) return;
    dispatch(
      displayAdditionalInfos({
        additionalInfos: getAdditionalInfos(node, parent),
      }),
    );
  };

  const onToggleAdditionalInfos = () => {
    if (!node.additionalInfos && isEmpty(node.matchingEntries)) return;

    dispatch(toggleAdditionalInfos());
    dispatch(
      displayAdditionalInfos({
        additionalInfos: getAdditionalInfos(node, parent),
      }),
    );
  };

  return (
    <Root
      className={className}
      onMouseEnter={onDisplayAdditionalInfos}
      onClick={onToggleAdditionalInfos}
    >
      {children}
    </Root>
  );
};

export default AdditionalInfoHoverable;
