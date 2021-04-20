import {
  setFilterValue,
  loadFilterSuggestionsSuccess,
} from "../../js/standard-query-editor/actions";
import reducer from "../../js/standard-query-editor/queryReducer";

const createQueryStateWithOneBigMultiSelect = () => [
  {
    elements: [
      {
        id: "elm0",
        tables: [
          {
            id: "elm0.tbl0",
            filters: [
              {
                id: "col0",
                type: "BIG_MULTI_SELECT",
                options: [],
              },
            ],
          },
        ],
        hasActiveFilters: false,
        isEditing: true, // This is the element we want to manipulate
      },
    ],
  },
];

describe("standard query editor", () => {
  describe("setting a filter value", () => {
    it("sets the value to what is given", () => {
      const state = createQueryStateWithOneBigMultiSelect();

      state[0].elements[0].tables[0].filters[0].options = [
        { value: 1, label: "1" },
      ];
      state[0].elements[0].tables[0].filters[0].value = [1];

      const action = setFilterValue(0, 0, []);
      const updatedState = reducer(state, action);

      expect(updatedState[0].elements[0].tables[0].filters[0].value).toEqual(
        [],
      );
    });
  });

  describe("receiving a list of autocomplete suggestions", () => {
    it("updates the filter's options list", () => {
      const state = createQueryStateWithOneBigMultiSelect();

      const options = [
        { value: "0", label: "0" },
        { value: "1", label: "1" },
        { value: "2", label: "2" },
      ];
      const action = loadFilterSuggestionsSuccess(options, 0, 0);
      const updatedState = reducer(state, action);

      expect(updatedState[0].elements[0].tables[0].filters[0].options).toEqual(
        options,
      );
    });

    it("leaves filter's options list unmodified when receiving empty suggestions list", () => {
      const options = [
        { value: "0", label: "0" },
        { value: "1", label: "1" },
        { value: "2", label: "2" },
      ];
      const state = createQueryStateWithOneBigMultiSelect();
      state[0].elements[0].tables[0].filters[0].options = options;

      const newOptions = [];
      const action = loadFilterSuggestionsSuccess(newOptions, 0, 0);
      const updatedState = reducer(state, action);

      expect(updatedState[0].elements[0].tables[0].filters[0].options).toEqual(
        options,
      );
    });
  });
});
