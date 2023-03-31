import styled from "@emotion/styled";
import { ReactNode } from "react";
import { useDispatch } from "react-redux";

import type { ConceptT } from "../api/types";
import { isEmpty } from "../common/helpers/commonHelper";
import { getNodeIcon } from "../model/node";

import { toggleAdditionalInfos, displayAdditionalInfos } from "./actions";
import { AdditionalInfosType } from "./reducer";

const Root = styled("div")`
  cursor: pointer;
`;

// Allowlist the data we pass (especially: don't pass all children)
const getAdditionalInfos = (
  node: ConceptT,
  root?: ConceptT,
): AdditionalInfosType => ({
  label: node.label,
  description: node.description,
  matchingEntries: node.matchingEntries,
  matchingEntities: node.matchingEntities,
  dateRange: node.dateRange,
  infos: node.additionalInfos,
  icon: getNodeIcon(node, {
    isStructNode: !root?.detailsAvailable,
  }),
  rootLabel: root?.label,
  rootIcon: root ? getNodeIcon(root) : undefined,
});

const AdditionalInfoHoverable = ({
  node,
  className,
  children,
  root,
}: {
  children: ReactNode;
  className?: string;
  node: ConceptT;
  root: ConceptT;
}) => {
  const dispatch = useDispatch();

  const onDisplayAdditionalInfos = () => {
    if (!node.additionalInfos && isEmpty(node.matchingEntries)) return;
    dispatch(
      displayAdditionalInfos({
        additionalInfos: getAdditionalInfos(node, root),
      }),
    );
  };

  const onToggleAdditionalInfos = () => {
    if (!node.additionalInfos && isEmpty(node.matchingEntries)) return;

    dispatch(toggleAdditionalInfos());
    dispatch(
      displayAdditionalInfos({
        additionalInfos: getAdditionalInfos(node, root),
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
