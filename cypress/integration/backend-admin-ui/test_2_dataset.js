/// <reference types="cypress" />
import { visitAdminUI } from "../../integration-helpers/visitAdminUI";

context("Admin UI Single Dataset", () => {
    const testDSLabel = "TestDatasetName";
    const testDSID = "TestDatasetID2";

    describe("Create a new dataset", () => {
        before(() => { visitAdminUI('datasets'); });

        it("Can create a new dataset", () => {
            cy.get('[data-test-id="entity-name"]').type(testDSLabel);
            cy.get('[data-test-id="entity-id"]').type(testDSID);
            cy.get('[data-test-id="create-dataset-btn"]').click().as('createDataset');
        });

        it("Can see the new dataset", () => {
            cy.contains(testDSID);
        });
    });

    describe("Access on the created dataset", () => {
        before(() => { visitAdminUI(`datasets/${testDSID}`); });

        it("Can see the datasets page", () => {
            cy.contains(`Dataset ${testDSLabel}`);
        });

        it("Can change the label", () => {
            cy.get('[data-test-id="editableText-btn"]').click();
            cy.get('[data-test-id="editableText-input"]').clear().type(`NEW ${testDSLabel}`);
            cy.get('[data-test-id="editableText-form"]').submit();

            cy.contains(`Dataset NEW ${testDSLabel}`);
        });
    });

    describe("Can upload test table and concept", () => {
        before(() => { visitAdminUI(`datasets/${testDSID}`); });

        it("Can upload test table", () => {
            cy.get('[data-test-id="upload-select"]').select('Table JSON');
            cy.get('[data-test-id="upload-input"]').selectFile('./cypress/support/test_data/all_types.table.json');
            cy.get('[data-test-id="upload-btn"]').click();
            cy.wait(3000).reload();
        });

        it("Is new table visible", () => {
            cy.get('[data-test-id="accordion-Tables"]').contains('td', `table`);
        });

        it("Can upload concept table", () => {
            cy.get('[data-test-id="upload-select"]').select('Concept JSON');
            cy.get('[data-test-id="upload-input"]').selectFile('./cypress/support/test_data/all_types.concept.json');
            cy.get('[data-test-id="upload-btn"]').click();
            cy.wait(3000).reload();
        });

        it("Is new concept visible", () => {
            cy.get('[data-test-id="accordion-Concepts"]').contains('td', `concept1`);
        });
    });

    describe("Can visit table info page", () => {
        before(() => { visitAdminUI(`datasets/${testDSID}/tables/${testDSID}.table`); });

        it("Can use page components", () => {
            cy.contains('Table table');
            cy.get('[data-test-id="accordion-Tags"]').click();
            cy.get('[data-test-id="accordion-Concepts"]').click();
            cy.get('[data-test-id="accordion-Columns"]').click();
        });
    });

    describe("Can visit concept info page", () => {
        before(() => { visitAdminUI(`datasets/${testDSID}/concepts/${testDSID}.concept1`); });

        it("Can use page components", () => {
            cy.contains('Concept Concept1');
            cy.get('[data-test-id="accordion-Selects"]').first().click();
            cy.get('[data-test-id="accordion-Connectors"]').click();
        });
    });

    describe("Can visit connector info page", () => {

        it("Can use page components", () => {
            visitAdminUI(`datasets/${testDSID}/concepts/${testDSID}.concept1`);
            cy.get('[data-test-id="accordion-Connectors"]').click();
            cy.get('[data-test-id="accordion-Connectors"]').get('tr > :nth-child(1) > a').click();

            cy.location('pathname').should('equal', `/admin-ui/datasets/${testDSID}/connectors/${testDSID}.concept1.column`);
        });

        it("Counts are right", () => {
            visitAdminUI(`datasets/${testDSID}/connectors/${testDSID}.concept1.column`);
            cy.get('[data-test-id="accordion-Filters"] > .card-header').contains("20 entries")
            cy.get('[data-test-id="accordion-Selects"] > .card-header').contains("16 entries")
        })
    });

    describe("Connector page links work", () => {
        beforeEach(() => { visitAdminUI(`datasets/${testDSID}/connectors/${testDSID}.concept1.column`); });

        it("Goto datasets", () => {
            cy.get('.breadcrumb > :nth-child(1) > a').click()
            cy.location('pathname').should('equal', `/admin-ui/datasets`);
        });

        it("Goto dataset", () => {
            cy.get('.breadcrumb > :nth-child(2) > a').click()
            cy.location('pathname').should('equal', `/admin-ui/datasets/${testDSID}`);
        });

        it("Goto concepts", () => {
            cy.get('.breadcrumb > :nth-child(3) > a').click()
            cy.location('pathname').should('equal', `/admin-ui/datasets/${testDSID}`).location('hash').should('equal','#Concepts');
        });

        it("Goto concept", () => {
            cy.get('.breadcrumb > :nth-child(4) > a').click()
            cy.location('pathname').should('equal', `/admin-ui/datasets/${testDSID}/concepts/${testDSID}.concept1`);
        });

        it("Goto connectors", () => {
            cy.get('.breadcrumb > :nth-child(5) > a').click()
            cy.location('pathname').should('equal', `/admin-ui/datasets/${testDSID}/concepts/${testDSID}.concept1`).location('hash').should('equal','#Connectors');
        });
    });

    describe("Can delete test concept and table", () => {
        beforeEach(() => visitAdminUI(`datasets/${testDSID}`));

        it("Can delete test concept", () => {
            cy.get('[data-test-id="accordion-Concepts"]').click();
            cy.get(`[data-test-id="delete-btn-concept-${testDSID}.concept1"]`).click({ force: true });
            cy.get(`[data-test-id="delete-btn-${testDSID}.concept1"]`).should('not.exist');
        });

        it("Can delete test table", () => {
            cy.get('[data-test-id="accordion-Tables"]').click();
            cy.get(`[data-test-id="delete-btn-table-${testDSID}.table"]`).click({ force: true });
            cy.get(`[data-test-id="delete-btn-${testDSID}.table"]`).should('not.exist');
        });
    });

    describe("Delete the test dataset", () => {
        before(() => { visitAdminUI('datasets'); });

        it("Can delete the test dataset", () => {
            cy.get(`[data-test-id="delete-btn-${testDSID}"]`).click({ force: true });
        });
    });
});