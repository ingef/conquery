import { expect } from "chai";
import thunk from "redux-thunk";
import configureMockStore from "redux-mock-store";
import nock from "nock";

import {
  selectConceptRootNode,
  selectConceptRootNodeAndResolveCodes,
  resolveConceptsStart,
  resolveConceptsSuccess,
  resolveConceptsError
} from "../../../lib/js/upload-concept-list-modal/actions";

import { apiUrl } from "../../../lib/js/environment";

const middlewares = [thunk];
const mockStore = configureMockStore(middlewares);

describe("upload concepts dialog", () => {
  describe("selecting a concept root node", () => {
    it("updates the selected node and performs an api request to retrieve concept ids", () => {
      const datasetId = 1;
      const conceptId = 1;
      const conceptCodes = ["foo", "bar"];
      const apiResponse = {
        resolved: conceptCodes.slice(0, 1),
        unresolved: conceptCodes.slice(1)
      };

      nock(apiUrl())
        .post(`/api/datasets/${datasetId}/concepts/${conceptId}/resolve`, {
          concepts: conceptCodes
        })
        .reply(200, { body: apiResponse });

      const expectedActions = [
        selectConceptRootNode(conceptId),
        resolveConceptsStart(),
        resolveConceptsSuccess({ body: apiResponse })
      ];
      const store = mockStore({});

      return store
        .dispatch(
          selectConceptRootNodeAndResolveCodes(
            datasetId,
            conceptId,
            conceptCodes
          )
        )
        .then(() => {
          const [
            setConceptRootNode,
            startApiRequest,
            completeApiRequest
          ] = store.getActions();

          expect(setConceptRootNode).to.deep.equal(expectedActions[0]);
          expect(startApiRequest).to.deep.equal(expectedActions[1]);

          expect(completeApiRequest.type).to.equal(expectedActions[2].type);
          expect(completeApiRequest.payload.data).to.deep.equal(
            expectedActions[2].payload.data
          );
          expect(completeApiRequest.payload.receivedAt).to.be.a("number");
        });
    });

    it("shows an error if an invalid concept is selected", () => {
      const datasetId = 1;
      const conceptId = 4711;
      const conceptCodes = ["foo", "bar"];
      const error = {
        status: 404,
        message: `There is no concept with the id ${conceptId}`
      };

      nock(apiUrl())
        .post(`/api/datasets/${datasetId}/concepts/${conceptId}/resolve`, {
          concepts: conceptCodes
        })
        .reply(404, error);

      const expectedActions = [
        selectConceptRootNode(conceptId),
        resolveConceptsStart(),
        resolveConceptsError(error)
      ];
      const store = mockStore({});

      return store
        .dispatch(
          selectConceptRootNodeAndResolveCodes(
            datasetId,
            conceptId,
            conceptCodes
          )
        )
        .then(() => {
          expect(store.getActions()).to.deep.equal(expectedActions);
        });
    });
  });
});
