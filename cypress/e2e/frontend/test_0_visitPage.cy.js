/// <reference types="cypress" />
import { visitWithToken } from "../../integration-helpers/visitWithToken";

const USER_TOKEN_WITHOUT_PERMISSIONS = "user.user1";
const USER_TOKEN_WITH_PERMISSIONS = "user.user2";

context("Visit conquery ", () => {
  describe("As authenticated user without permissions", () => {
    beforeEach(() => {
      visitWithToken(USER_TOKEN_WITHOUT_PERMISSIONS);
    });

    it("Cannot see concepts", () => {
      cy.get('[data-test-id="left-pane"]').contains("Konzepte");

      cy.contains("Keine Konzepte");
    });

    it("Cannot see queries", () => {
      cy.get('[data-test-id="left-pane"]').contains("Anfragen").click();

      cy.contains("Keine Anfragen / Formulare gefunden");
    });
  });

  describe("As authenticated user with permissions", () => {
    beforeEach(() => {
      visitWithToken(USER_TOKEN_WITH_PERMISSIONS);
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
  });
});
