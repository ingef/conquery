/// <reference types="cypress" />
import { visitWithToken } from "../../integration-helpers/visitWithToken";

const USER_TOKEN_WITH_PERMISSIONS = "user.user2";

describe("Run query", () => {
  beforeEach(() => {
    // run these tests as if in a desktop
    // browser with a 720p monitor
    cy.viewport(1280, 720)
    
    visitWithToken(USER_TOKEN_WITH_PERMISSIONS);
  });

  it("Can execute query and see it in the queries tab", () => {
    cy.get('[data-test-id="right-pane-container"] >div:visible').as("queryEditor");

    // Drag concept to editor
    cy.contains("Concept1").trigger("dragstart").trigger("dragleave");
    cy.get("@queryEditor")
      .trigger("dragenter")
      .trigger("dragover")
      .trigger("drop")
      .trigger("dragend");

    // Set money filter to a negative value
    cy.get("@queryEditor").contains("Concept1").click()
    cy.get("@queryEditor")
      .find('[data-test-id="table-filter-dataset1.concept1.column.sum_money"]').as("money-filter")

    cy.get("@money-filter").find('input').first().as("money-min-input")
    cy.get("@money-filter").scrollIntoView()
    // Unit should be automatically added
    cy.get("@money-min-input").type("-4").should('have.value', '-4 €')

    // Save the settings
    cy.get("@queryEditor").contains("Speichern").click()

    // Start query
    cy.get("@queryEditor").find('[data-test-id="query-runner-button"]').click();

    cy.get("@queryEditor").contains("Ergebnisse");
  });

  it("Can see the executed query in the queries tab", () => {
    cy.get('[data-test-id="left-pane"]').contains("Anfragen").click();

    cy.get('[data-test-id="left-pane-container"]').as("leftPaneContainer");

    cy.get("@leftPaneContainer").contains("Ergebnisse");
    cy.get("@leftPaneContainer").contains("Concept1");
  });

  it("Can delete the query", () => {
    cy.get('[data-test-id="left-pane"]').contains("Anfragen").click();

    cy.get('[data-test-id="left-pane-container"]').as("leftPaneContainer");

    cy.get('[data-test-id="project-items-list"]').as("executionList");
    
    cy.get('@executionList').find('[data-test-id="project-item-delete-button"]').click();
    cy.get('@executionList').contains('Anfrage jetzt löschen').click();
  });
});
