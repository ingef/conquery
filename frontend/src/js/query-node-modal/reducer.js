import { toUpperCaseUnderscore } from '../common/helpers';
import * as actionTypes          from './actionTypes';

export default function createQueryNodeModalReducer(type) {
  const initialState = {
    andIdx: null,
    orIdx: null
  };

  const uppercasedType = toUpperCaseUnderscore(type);

  const SET_NODE = actionTypes[`SET_${uppercasedType}_NODE`];
  const CLEAR_NODE = actionTypes[`CLEAR_${uppercasedType}_NODE`];

  return (state = initialState, action) => {
    switch (action.type) {
      case SET_NODE:
        return {
          ...state,
          andIdx: action.payload.andIdx,
          orIdx: action.payload.orIdx
        };
      case CLEAR_NODE:
        return {
          ...state,
          andIdx: null,
          orIdx: null
        };
      default:
        return state;
    }
  };
}
