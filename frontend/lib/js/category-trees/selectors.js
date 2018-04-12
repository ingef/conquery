// @flow

import { SearchType } from "./reducer";

export const isTreeNodeInSearchResult = (treeId, id, search: SearchType): boolean => {
    if (search && search.searching) {
        const result = search.result[treeId];
        if (id === null) return !!result; // render structure nodes

        return result ? result.includes(id) : false;
    }
    return true;
}
