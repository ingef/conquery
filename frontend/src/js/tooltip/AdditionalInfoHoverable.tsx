import { connect } from "react-redux";

import type { DateRangeT, InfoT } from "../api/types";
import { isEmpty } from "../common/helpers";

import HoverableBase from "./HoverableBase";
import { toggleAdditionalInfos, displayAdditionalInfos } from "./actions";

export type AdditionalInfoHoverableNodeType = {
  label: string;
  description: string;
  children?: Array<string>;
  matchingEntries: number;
  dateRange: DateRangeT;
  additionalInfos: InfoT[];
};

// Allowlist the data we pass (especially: don't pass all children)
const additionalInfos = (node: AdditionalInfoHoverableNodeType) => ({
  label: node.label,
  description: node.description,
  isFolder: !!node.children && node.children.length > 0,
  matchingEntries: node.matchingEntries,
  dateRange: node.dateRange,
  infos: node.additionalInfos,
});

// Decorates a component with a hoverable node.
// On mouse enter, additional infos about the component are saved in the state
// The Tooltip (and potential other components) might then update their view.
// On mouse leave, the infos are cleared from the state again
const AdditionalInfoHoverable = (Component: any) => {
  const mapStateToProps = () => ({});

  const mapDispatchToProps = (
    dispatch: Dispatch,
    ownProps: { node: AdditionalInfoHoverableNodeType },
  ) => ({
    onDisplayAdditionalInfos: () => {
      const node = ownProps.node;

      if (!node.additionalInfos && isEmpty(node.matchingEntries)) return;

      dispatch(displayAdditionalInfos(additionalInfos(node)));
    },
    onToggleAdditionalInfos: () => {
      const node = ownProps.node;

      if (!node.additionalInfos && isEmpty(node.matchingEntries)) return;

      dispatch([
        toggleAdditionalInfos(),
        displayAdditionalInfos(additionalInfos(node)),
      ]);
    },
  });

  return connect(mapStateToProps, mapDispatchToProps)(HoverableBase(Component));
};

export default AdditionalInfoHoverable;
