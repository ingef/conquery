/// <reference types="cypress" />
import { visitAdminUI } from "../../integration-helpers/visitAdminUI";

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

    describe("Dataset pages render", () => {
      
      it("Datasets", () => {
        
        visitAdminUI("datasets");

        cy.root().should('not.contain.text', 'FreeMarker template error')

        cy.get('[data-cy=datasets-dataset1]')
          .find('a')
          .contains('dataset1')
          .click()


        cy.get('[data-test-id="accordion-Mappings"]').click()
        cy.get('[data-test-id="accordion-SearchIndices"]').click()
        cy.get('[data-test-id="accordion-Tables"]').click()
        cy.get('[data-test-id="accordion-Concepts"]').click()
        cy.get('[data-test-id="accordion-SecondaryIds"]').click()
        
        cy.root().should('not.contain.text', 'FreeMarker template error')
        
        // Table page
        cy.get('[data-test-id="accordion-Tables"]')
          .find('a')
          .contains('table')
          .click()

        cy.get('[data-test-id="accordion-Tags"]').click()
        cy.get('[data-test-id="accordion-Concepts"]').click()
        cy.get('[data-test-id="accordion-Columns"]').click()
        
        cy.root().should('not.contain.text', 'FreeMarker template error')

        // Import page
        cy.get('[data-test-id="accordion-Tags"]')
          .find('a')
          .contains('table')
          .click()
          
          cy.root().should('not.contain.text', 'FreeMarker template error')


      });
    });
})  