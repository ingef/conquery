import styled from "@emotion/styled";
import { FC } from "react";
import { useDispatch } from "react-redux";

import type { ConceptT, DateRangeT, InfoT } from "../api/types";
import { isEmpty } from "../common/helpers";

import { toggleAdditionalInfos, displayAdditionalInfos } from "./actions";
import { AdditionalInfosType } from "./reducer";

const Root = styled("div")`
  cursor: pointer;
`;

export type AdditionalInfoHoverableNodeType = {
  label: string;
  description?: string;
  children?: string[];
  matchingEntries: number;
  matchingEntities: number;
  dateRange?: DateRangeT;
  additionalInfos: InfoT[];
};

// Allowlist the data we pass (especially: don't pass all children)
const getAdditionalInfos = (node: ConceptT): AdditionalInfosType => ({
  label: node.label,
  description: node.description,
  isFolder: !!node.children && node.children.length > 0,
  matchingEntries: node.matchingEntries,
  matchingEntities: node.matchingEntities,
  dateRange: node.dateRange,
  infos: node.additionalInfos,
});

interface Props {
  node: ConceptT;
  className?: string;
}

const AdditionalInfoHoverable: FC<Props> = ({ node, className, children }) => {
  const dispatch = useDispatch();

  const onDisplayAdditionalInfos = () => {
    if (!node.additionalInfos && isEmpty(node.matchingEntries)) return;

    dispatch(
      displayAdditionalInfos({ additionalInfos: getAdditionalInfos(node) }),
    );
  };

  const onToggleAdditionalInfos = () => {
    if (!node.additionalInfos && isEmpty(node.matchingEntries)) return;

    dispatch([
      toggleAdditionalInfos(),
      displayAdditionalInfos({ additionalInfos: getAdditionalInfos(node) }),
    ]);
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
