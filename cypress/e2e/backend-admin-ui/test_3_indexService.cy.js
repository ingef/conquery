/// <reference types="cypress" />
import { visitAdminUI } from "../../integration-helpers/visitAdminUI";

context("Visit Index Service Page", () => {
    describe("Perform Update Matching Stats", () => {

        it("Start Update Matching Stats", () => {
            visitAdminUI("datasets");
            cy.get('[data-cy="datasets"]').contains('dataset1').click();
            cy.get('[data-cy="update-matching-stats"]').click();
        })

        it("Wait for Update Matching Stats to finish", () => {
            visitAdminUI("jobs");
            cy.get('#origin_Manager > .card-header > .row > .accordion-infotext > div > .jobsAmount').eq(0);
        })
    });
    describe("Visit Index Service Page", () => {

        it("Check Statistics", () => {
            visitAdminUI("index-service");

            cy.get('[data-cy="statistics"]').contains('Miss count').next().should('not.equal', 0);
            cy.get('[data-cy="statistics"]').contains('Total load time').next().should('not.equal', 0);
            
            cy.get('[data-cy="indexes"]').contains('mapping_data.csv')
                .next().contains("internal")
                .next().contains('{{external}}');
        })
    });
})