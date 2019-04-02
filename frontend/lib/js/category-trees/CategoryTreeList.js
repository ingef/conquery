// @flow

import React from "react";
import styled from "@emotion/styled";
import { connect } from "react-redux";
import T from "i18n-react";

import type { StateType } from "../app/reducers";
import { ErrorMessage } from "../error-message";

import { getConceptById } from "./globalTreeStoreHelper";
import { type TreesType, type SearchType } from "./reducer";
import CategoryTree from "./CategoryTree";
import CategoryTreeFolder from "./CategoryTreeFolder";
import { isNodeInSearchResult } from "./selectors";

const Root = styled("div")`
  flex-grow: 1;
  flex-shrink: 0;
  flex-basis: 0;
  overflow-y: auto;
  padding: 0 10px 0 20px;
  white-space: nowrap;

  // Only hide the category trees when the tab is not selected
  // Because mount / unmount would reset the open states
  // that are React states and not part of the Redux state
  // because if they were part of Redux state, the entire tree
  // would have to re-render when a single node would be opened
  //
  // Also: Can't set it to initial, because IE11 doesn't work then
  // => Empty string instead
  display: ${({ show }) => (show ? "" : "none")};
`;

type PropsType = {
  trees: TreesType,
  activeTab: string,
  search?: SearchType
};

const StyledErrorMessage = styled(ErrorMessage)`
  padding-left: 20px;
  font-size: ${({ theme }) => theme.font.sm};
  margin: 2px 0;
`;

class CategoryTreeList extends React.Component<PropsType> {
  props: PropsType;

  render() {
    const { activeTab, search, trees } = this.props;

    return (
      !search.loading && (
        <Root show={activeTab === "categoryTrees"}>
          {this.props.trees ? (
            Object.keys(this.props.trees)
              // Only take those that don't have a parent, they must be root
              .filter(treeId => !trees[treeId].parent)
              .sort((a, b) => {
                const aTree = trees[a];
                const bTree = trees[b];

                if (!!aTree.children === !!bTree.children) {
                  return aTree.label.localeCompare(bTree.label);
                }

                return !!aTree.children ? -1 : 1;
              })
              .map((treeId, i) => {
                const tree = trees[treeId];
                const rootConcept = getConceptById(treeId);

                const render = isNodeInSearchResult(
                  treeId,
                  tree.children,
                  search
                );

                if (!render) return null;

                return tree.detailsAvailable ? (
                  <CategoryTree
                    key={i}
                    id={treeId}
                    label={tree.label}
                    tree={rootConcept}
                    treeId={treeId}
                    loading={!!tree.loading}
                    error={tree.error}
                    depth={0}
                    search={search}
                  />
                ) : (
                  <CategoryTreeFolder
                    key={i}
                    trees={trees}
                    tree={tree}
                    treeId={treeId}
                    depth={0}
                    active={tree.active}
                    openInitially
                    search={search}
                  />
                );
              })
          ) : (
            <StyledErrorMessage
              message={T.translate("categoryTreeList.noTrees")}
            />
          )}
        </Root>
      )
    );
  }
}

const mapStateToProps = (state: StateType) => {
  return {
    trees: state.categoryTrees.trees,
    activeTab: state.panes.left.activeTab,
    search: state.categoryTrees.search
  };
};

export default connect(mapStateToProps)(CategoryTreeList);
