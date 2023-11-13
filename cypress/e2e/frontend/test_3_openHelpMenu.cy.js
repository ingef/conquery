/// <reference types="cypress" />
import { visitWithToken } from "../../integration-helpers/visitWithToken";

const USER_TOKEN_WITH_PERMISSIONS = "user.user2";

describe("Visit Form Editor", () => {
  beforeEach(() => {
    visitWithToken(USER_TOKEN_WITH_PERMISSIONS);
  });

  it("Can open help menu", () => {
    cy.get('[data-test-id="help-menu"]').click();

    cy.get('[data-test-id="help-manual"]').should("have.attr", "href", "https://example.org");

    cy.get('[data-test-id="help-email"]').should("have.attr", "href", "mailto:test@example.org");
  });
});
