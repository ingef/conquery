/// <reference types="cypress" />
import { visitWithToken } from "../../integration-helpers/visitWithToken";

const USER_TOKEN_WITH_PERMISSIONS = "user.user2";

describe("Visit Form Editor", () => {
  before(() => {
    visitWithToken(USER_TOKEN_WITH_PERMISSIONS);
  });

  it("Can open the form editor tab", () => {
    cy.get('[data-test-id="right-pane"]').contains("Formular-Editor").click();

    cy.get('[data-test-id="right-pane-container"]').as("formEditor");

    cy.get("@formEditor").contains("Formulare");
    cy.get('[data-test-id="form-select"]').should("be.visible");
  });
});
