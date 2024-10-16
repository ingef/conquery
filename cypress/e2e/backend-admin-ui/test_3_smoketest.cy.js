/// <reference types="cypress" />
import {visitAdminUI} from "../../integration-helpers/visitAdminUI";

context("Simplest Smoke Tests", () => {

    describe("UI renders", () => {

        it("Query Overview", () => {

            visitAdminUI("queries");

            // Disable updates so the test ends
            cy.get('#updateCheckBox').click()
            cy.root().should('not.contain.text', 'FreeMarker template error')
        });


        it("Jobs Overview", () => {

            visitAdminUI("jobs");

            cy.root().should('not.contain.text', 'FreeMarker template error')
        });
    });
})  
