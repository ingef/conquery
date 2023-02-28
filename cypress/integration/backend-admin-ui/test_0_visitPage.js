/// <reference types="cypress" />
import { visitAdminUI } from "../../integration-helpers/visitAdminUI";

context("Visit conquery admin ui", () => {
    describe("Access on the start page", () => {
        before(() => { visitAdminUI(); });

        it("Can see the start page", () => {
            cy.contains("Conquery Admin");
        });
    });
});