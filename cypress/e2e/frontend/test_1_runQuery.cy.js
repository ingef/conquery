/// <reference types="cypress" />
import { visitWithToken } from "../../integration-helpers/visitWithToken";

const USER_TOKEN_WITH_PERMISSIONS = "user.user2";

describe("Run query", () => {
  before(() => {
    visitWithToken(USER_TOKEN_WITH_PERMISSIONS);
  });

  it("Can execute query and see it in the queries tab", () => {
    cy.get('[data-test-id="right-pane-container"] >div:visible').as(
      "queryEditor",
    );

    cy.contains("Concept1").trigger("dragstart").trigger("dragleave");
    cy.get("@queryEditor")
      .trigger("dragenter")
      .trigger("dragover")
      .trigger("drop")
      .trigger("dragend");

    cy.get("@queryEditor").find('[data-test-id="query-runner-button"]').click();

    cy.get("@queryEditor").contains("Ergebnisse");
  });

  it("Can see the executed query in the queries tab", () => {
    cy.get('[data-test-id="left-pane"]').contains("Anfragen").click();

    cy.get('[data-test-id="left-pane-container"]').as("leftPaneContainer");

    cy.get("@leftPaneContainer").contains("Ergebnisse");
    cy.get("@leftPaneContainer").contains("Concept1");
  });
});
