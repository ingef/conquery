import styled from "@emotion/styled";

import type { ConceptElementT, ConceptIdT, ConceptT } from "../api/types";
import { useOpenableConcept } from "../concept-trees-open/useOpenableConcept";
import { resetSelects } from "../model/select";
import { resetTables } from "../model/table";
import type { ConceptQueryNodeType } from "../standard-query-editor/types";

import ConceptTreeNodeTextContainer from "./ConceptTreeNodeTextContainer";
import { getConceptById } from "./globalTreeStoreHelper";
import type { SearchT } from "./reducer";
import { isNodeInSearchResult } from "./selectors";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
`;

const ConceptTreeNode = ({
  data,
  rootConceptId,
  conceptId,
  depth,
  search,
}: {
  rootConceptId: ConceptIdT;
  conceptId: ConceptIdT;
  data: ConceptT;
  depth: number;
  search: SearchT;
}) => {
  const { open, onToggleOpen } = useOpenableConcept({
    conceptId,
  });

  function toggleOpen() {
    if (data.children?.length === 0) return;

    onToggleOpen();
  }

  if (!search.showMismatches) {
    const shouldRender = isNodeInSearchResult(conceptId, search, data.children);

    if (!shouldRender) return null;
  }

  const isOpen = open || search.allOpen;

  const root = getConceptById(
    rootConceptId,
    rootConceptId, // To optimize lookup
  ) as ConceptElementT;

  return (
    <Root>
      <ConceptTreeNodeTextContainer
        node={{
          label: data.label,
          description: data.description,

          additionalInfos: data.additionalInfos,
          matchingEntries: data.matchingEntries,
          matchingEntities: data.matchingEntities,
          dateRange: data.dateRange,

          children: data.children,
        }}
        root={root}
        conceptId={conceptId}
        createQueryElement={(): ConceptQueryNodeType => {
          const description = data.description
            ? { description: data.description }
            : {};

          return {
            ids: [conceptId],
            ...description,
            label: data.label,
            tables: root?.tables
              ? resetTables(root.tables, { useDefaults: true })
              : [],
            selects: root?.selects
              ? resetSelects(root.selects, { useDefaults: true })
              : [],

            additionalInfos: data.additionalInfos,
            matchingEntries: data.matchingEntries,
            matchingEntities: data.matchingEntities,
            dateRange: data.dateRange,

            excludeTimestamps:
              root.excludeFromTimeAggregation ||
              data.excludeFromTimeAggregation,

            tree: rootConceptId,
          };
        }}
        open={isOpen}
        depth={depth}
        active={data.active}
        onTextClick={toggleOpen}
        search={search}
      />
      {!!data.children && isOpen && (
        <>
          {data.children.map((childId) => {
            const child = getConceptById(childId);

            return child ? (
              <ConceptTreeNode
                key={childId}
                rootConceptId={rootConceptId}
                conceptId={childId}
                data={child}
                depth={depth + 1}
                search={search}
              />
            ) : null;
          })}
        </>
      )}
    </Root>
  );
};

export default ConceptTreeNode;
