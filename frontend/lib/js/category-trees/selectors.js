// @flow

import { difference }           from 'lodash'

import { type TreeNodeIdType }  from '../common/types/backend';
import { type SearchType }      from './reducer';

const isSearchResultInChildren = (children?: [], search?: SearchType) => {
    if (!search || !search.result || !children) return false;
    const result = search.result;

    for (var i = 0; i < result.length; i++) {
      const ids = result[i].split('.');
      for (var j = 0; j < children.length; j++) {
        const childIds = children[j].split('.');

        if (difference(childIds, ids).length === 0)
          return true;
      }
    }
    return false;
}

export const isInSearchResult = (id: TreeNodeIdType, children?: [], search?: SearchType) => {
    if (!search || !search.result) return false;
    const result = search.result;

    if (result.includes(id)) return true;

    if (children && children.length > 0)
        return isSearchResultInChildren(children, search);

    return false;
}
