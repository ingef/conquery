import React from "react";
import styled from "@emotion/styled";

import type { ConceptIdT, InfoT, DateRangeT, ConceptT } from "../api/types";

import { selectsWithDefaults } from "../model/select";
import { tablesWithDefaults } from "../model/table";

import type { DraggedNodeType } from "../standard-query-editor/types";
import type { SearchT } from "./reducer";

import { getConceptById } from "./globalTreeStoreHelper";
import Openable from "./Openable";
import ConceptTreeNodeTextContainer from "./ConceptTreeNodeTextContainer";
import { isNodeInSearchResult } from "./selectors";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
`;

// Concept data that is necessary to display tree nodes. Includes additional infos
// for the tooltip as well as the id of the corresponding tree
type TreeNodeData = {
  label: string;
  description: string;
  active: boolean;
  matchingEntries: number;
  dateRange: DateRangeT;
  additionalInfos: InfoT[];
  children: ConceptIdT[];

  tree: ConceptIdT;
};

type PropsType = {
  id: ConceptIdT;
  data: TreeNodeData;
  depth: number;
  open: boolean;
  search: SearchT;
  onToggleOpen: () => void;
};

const selectTreeNodeData = (concept: ConceptT, tree: ConceptIdT) => ({
  label: concept.label,
  description: concept.description,
  active: concept.active,
  matchingEntries: concept.matchingEntries,
  dateRange: concept.dateRange,
  additionalInfos: concept.additionalInfos,
  children: concept.children,

  tree
});

class ConceptTreeNode extends React.Component<PropsType> {
  onToggleOpen = () => {
    if (!this.props.data.children) return;

    this.props.onToggleOpen();
  };

  render() {
    const { id, data, depth, open, search } = this.props;

    if (!search.showMismatches) {
      const shouldRender = isNodeInSearchResult(id, data.children, search);

      if (!shouldRender) return null;
    }

    const isOpen = open || search.allOpen;

    return (
      <Root>
        <ConceptTreeNodeTextContainer
          node={{
            id,
            label: data.label,
            description: data.description,

            additionalInfos: data.additionalInfos,
            matchingEntries: data.matchingEntries,
            dateRange: data.dateRange,

            children: data.children
          }}
          createQueryElement={(): DraggedNodeType => {
            const { tables, selects } = getConceptById(data.tree);

            const description = data.description
              ? { description: data.description }
              : {};

            return {
              ids: [id],
              ...description,
              label: data.label,
              tables: tablesWithDefaults(tables),
              selects: selectsWithDefaults(selects),

              additionalInfos: data.additionalInfos,
              matchingEntries: data.matchingEntries,
              dateRange: data.dateRange,

              tree: data.tree
            };
          }}
          open={isOpen}
          depth={depth}
          active={data.active}
          onTextClick={this.onToggleOpen}
          search={search}
        />
        {!!data.children && isOpen && (
          <>
            {data.children.map((childId, i) => {
              const child = getConceptById(childId);

              return child ? (
                <OpenableConceptTreeNode
                  key={i}
                  id={childId}
                  data={selectTreeNodeData(child, data.tree)}
                  depth={this.props.depth + 1}
                  search={this.props.search}
                />
              ) : null;
            })}
          </>
        )}
      </Root>
    );
  }
}

const OpenableConceptTreeNode = Openable(ConceptTreeNode);

export default OpenableConceptTreeNode;
