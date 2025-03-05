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

  it("Can execute query, see it in the queries tab and delete it", () => {
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

    // Lookup executed query in the previous queries tab
    cy.get('[data-test-id="left-pane"]').contains("Anfragen").click();

    cy.get('[data-test-id="left-pane-container"]').as("leftPaneContainer");

    cy.get("@leftPaneContainer").contains("Ergebnisse");
    cy.get("@leftPaneContainer").contains("Concept1");

    // Delete the Query
    cy.get('[data-test-id="left-pane"]').contains("Anfragen").click();

    cy.get('[data-test-id="left-pane-container"]').as("leftPaneContainer");

    cy.get('[data-test-id="project-items-list"]').as("executionList");
    
    cy.get('@executionList').find('[data-test-id="project-item-delete-button"]').click();
    cy.get('@executionList').contains('Anfrage jetzt löschen').click();

    cy.get('@leftPaneContainer').contains('Keine Anfragen / Formulare gefunden')
  });

  it("Check user error message", () => {
    cy.get('[data-test-id="right-pane-container"] >div:visible').as("queryEditor");

    // Drag concept to editor
    cy.contains("MultiConnector").trigger("dragstart").trigger("dragleave");
    cy.get("@queryEditor")
      .trigger("dragenter")
      .trigger("dragover")
      .trigger("drop")
      .trigger("dragend");

    // Switch to secondary id mode
    cy.get("@queryEditor").contains("Secondary Id").click()

    // Exclude only concept from secondary id to create an invalid query
    cy.get("@queryEditor").find('[data-test-id="secondary-id-toggle"]').click()

    // Start query
    cy.get("@queryEditor").find('[data-test-id="query-runner-button"]').click();

    // Check for specific user error message
    cy.get('[data-test-id="query-runner"]').contains("Die ausgewählte Analyseebenen konnte in keinem der ausgewählten Konzepten gefunden werden.")
  })
});

describe("Reference list", () => {
  beforeEach(() => {
    // run these tests as if in a desktop
    // browser with a 720p monitor
    cy.viewport(1280, 720)
    
    visitWithToken(USER_TOKEN_WITH_PERMISSIONS);
  });

  it("Use reference list to resolve concept", () =>{
    cy.get('[data-test-id="right-pane-container"] >div:visible').as("queryEditor");

    // We need force here because the input is invisible
    cy.get("@queryEditor").get('input[type=file]').selectFile('cypress/support/test_data/concept_reference_list.txt', {"force": true})
    cy.get('@queryEditor')
      .find('[data-test-id="uploadConceptListModal"]')
      .as("uploadConceptListModal")
      .find('[data-test-id="selection-dropdown"]').click()

    // Choose a concept
    cy.get('@uploadConceptListModal')
    .find('[data-test-id="select-options"]').contains("MultiConnector").first().click()

    // We expect that one value 'b' cannot be resolved
    cy.get('@uploadConceptListModal').contains("1 Wert nicht aufgelöst")
    cy.get('@uploadConceptListModal').find('[data-test-id="unresolvable-list"]').contains('b')
    // 'a1' can be resolved
    cy.get('@uploadConceptListModal').contains("1 Wert aufgelöst.")

    // Change list name
    cy.get('@uploadConceptListModal').find('[data-test-id="insert-form"]').as("insert-form")
    cy.get('@insert-form').find('input[type=text]').should('have.value', 'concept_reference_list')
    cy.get('@insert-form').find('button[type=button]').click()
    cy.get('@insert-form').find('input[type=text]').type("My List")

    // Insert elements
    cy.get('@uploadConceptListModal').find('[data-test-id="insert"]').click()

    // Check that node was inserted in query editor
    cy.get('@queryEditor').find('[data-test-id="query-group"]').contains("MultiConnector")
    cy.get('@queryEditor').find('[data-test-id="query-group"]').contains("My List")

    // Clear editor
    cy.get('@queryEditor').find('svg[data-icon="trash"]').click()
    cy.get('@queryEditor').find('button[data-test-id="confirm"]').click()
    cy.get('@queryEditor').find('[data-test-id="text-initial"]')
  })

  it("Use reference list to resolve filter values", () =>{
    cy.get('[data-test-id="right-pane-container"] >div:visible').as("queryEditor");

    // We need force here because the input is invisible
    cy.get("@queryEditor").get('input[type=file]').selectFile('cypress/support/test_data/filter_value_reference_list.txt', {"force": true})
    cy.get('@queryEditor')
      .find('[data-test-id="uploadConceptListModal"]')
      .as("uploadConceptListModal")
      .find('[data-test-id="selection-dropdown"]').click()

    // Choose a concept
    cy.get('@uploadConceptListModal')
    .find('[data-test-id="select-options"]').contains("connector1").first().click()

    // We expect that one value 'b' cannot be resolved
    cy.get('@uploadConceptListModal').contains("1 Wert nicht aufgelöst")
    cy.get('@uploadConceptListModal').find('[data-test-id="unresolvable-list"]').contains('b')
    // 'a1' can be resolved
    cy.get('@uploadConceptListModal').contains("2 Werte aufgelöst.")

    // Change list name
    cy.get('@uploadConceptListModal').find('[data-test-id="insert-form"]').as("insert-form")
    cy.get('@insert-form').find('input[type=text]').should('have.value', 'filter_value_reference_list')
    cy.get('@insert-form').find('button[type=button]').click()
    cy.get('@insert-form').find('input[type=text]').type("My List")

    // Insert elements
    cy.get('@uploadConceptListModal').find('[data-test-id="insert"]').click()

    // Check that node was inserted in query editor
    cy.get('@queryEditor').find('[data-test-id="query-group"]').contains("MultiConnector").click()

    // Check that filter values are set corretly
    cy.get("@queryEditor")
      .find('[data-test-id="table-filter-dataset1.multiconnector.connector1.big_multi_select"]').as("multi_select")
      cy.get("@multi_select").scrollIntoView()
      cy.get("@multi_select").find('p').eq(0).contains('a')
      cy.get("@multi_select").find('p').eq(1).contains('abc')
  })
})