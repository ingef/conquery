/// <reference types="cypress" />

context("Visit conquery as users with different permissions", () => {
  describe("Visit as a authenticated user without permissions", () => {
    before(() => {
      cy.intercept({
        method: "GET",
        url: "/api/config/frontend",
      }).as("config");
      cy.intercept({
        method: "GET",
        url: "/api/datasets",
      }).as("datasets");
      cy.intercept({
        method: "GET",
        url: "/api/me",
      }).as("meInfo");

      cy.visit("http://localhost:8000/?access_token=user.user1");

      cy.wait(["@config", "@datasets", "@meInfo"]);
    });

    it("Cannot see concepts", () => {
      cy.get('[data-test-id="left-pane"]').contains("Konzepte");

      cy.contains("Keine Konzepte");
    });

    it("Cannot see queries", () => {
      cy.get('[data-test-id="left-pane"]').contains("Anfragen").click();

      cy.contains("Keine Anfragen gefunden");
    });

    it("Cannot see forms", () => {
      cy.get('[data-test-id="left-pane"]').contains("Formulare").click();

      cy.contains("Keine Formular-Konfigurationen gefunden");
    });
  });

  describe("Visit as a authenticated user with permissions", () => {
    before(() => {
      cy.intercept({
        method: "GET",
        url: "/api/config/frontend",
      }).as("config");
      cy.intercept({
        method: "GET",
        url: "/api/datasets",
      }).as("datasets");
      cy.intercept({
        method: "GET",
        url: "/api/me",
      }).as("meInfo");

      cy.visit("http://localhost:8000/?access_token=user.user2");

      cy.wait(["@config", "@datasets", "@meInfo"]);
    });

    it("Can see dataset", () => {
      cy.get('[data-test-id="dataset-selector"]')
        .get("input")
        .invoke("attr", "value")
        .should("eq", "Dataset1");
    });

    it("Can see concepts", () => {
      cy.contains("Concept1");
    });

    it("Can execute query", () => {

      cy.get('[data-test-id="right-pane-container"]')
        .find('>div')
        .filter(':visible')
        .as('queryEditor')

      cy.contains("Concept1").trigger("dragstart").trigger("dragleave");
      cy.get('@queryEditor')
        .trigger("dragenter")
        .trigger("dragover")
        .trigger("drop")
        .trigger("dragend");

      cy.get('@queryEditor')
        .find('[data-test-id="query-runner-button"]').click()

      
      cy.get('@queryEditor').contains("Ergebnisse")
    });
  });
});
