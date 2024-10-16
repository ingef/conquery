import {useMemo} from "react";
import type {ConceptIdT} from "../api/types";

import ConceptTree from "./ConceptTree";
import ConceptTreeFolder, {getNonFolderChildren} from "./ConceptTreeFolder";
import {getConceptById} from "./globalTreeStoreHelper";
import type {SearchT, TreesT} from "./reducer";
import {isNodeInSearchResult} from "./selectors";

const ConceptTreeListItem = ({
                                 trees,
                                 conceptId,
                                 search,
                                 onLoadTree,
                             }: {
    trees: TreesT;
    conceptId: ConceptIdT;
    search: SearchT;
    onLoadTree: (id: string) => void;
}) => {
    const tree = trees[conceptId];

    const nonFolderChildren = useMemo(
        () =>
            tree.detailsAvailable
                ? tree.children
                : getNonFolderChildren(trees, tree, conceptId),
        [trees, tree, conceptId],
    );

    if (!isNodeInSearchResult(conceptId, search, nonFolderChildren)) return null;

    const rootConcept = getConceptById(conceptId);

    const commonProps = {
        conceptId,
        search,
        onLoadTree,
        depth: 0,
    };

    return tree.detailsAvailable ? (
        <ConceptTree
            label={tree.label}
            tree={rootConcept}
            loading={!!tree.loading}
            error={tree.error}
            {...commonProps}
        />
    ) : (
        <ConceptTreeFolder
            trees={trees}
            tree={tree}
            active={tree.active}
            openInitially
            {...commonProps}
        />
    );
};

export default ConceptTreeListItem;
