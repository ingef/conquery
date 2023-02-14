/// <reference types="cypress" />
import { visitAdminUI } from "../../integration-helpers/visitAdminUI";

context("Admin UI Single Dataset", () => {
    const testDSLabel = "TestDatasetName";
    const testDSID = "TestDatasetID";

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
            cy.get('[data-test-id="dataset-label-input"]').clear().type(`NEW ${testDSLabel}`);
            cy.get('[data-test-id="dataset-label-btn"]').click();

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
            cy.get('[data-test-id="accordion-Tables"]').contains('td', `${testDSID}.table`);
        });

        it("Can upload concept table", () => {
            cy.get('[data-test-id="upload-select"]').select('Concept JSON');
            cy.get('[data-test-id="upload-input"]').selectFile('./cypress/support/test_data/all_types.concept.json');
            cy.get('[data-test-id="upload-btn"]').click();
            cy.wait(3000).reload();
        });

        it("Is new concept visible", () => {
            cy.get('[data-test-id="accordion-Concepts"]').contains('td', `${testDSID}.concept1`);
        });
    });

    describe("Can delete test table and concept", () => {

        it("Can delete test concept", () => {
            cy.get('[data-test-id="accordion-Concepts"]').click();
            cy.get(`[data-test-id="delete-btn-concept-${testDSID}.concept1"]`).click({force: true});
        });

        it("Can delete test table", () => {
            cy.get('[data-test-id="accordion-Tables"]').click();
            cy.get(`[data-test-id="delete-btn-table-${testDSID}.table"]`).click({force: true});
        });

    });

    describe("Delete the test dataset", () => {
        before(() => { visitAdminUI('datasets'); });

        it("Can delete the test dataset", () => {
            cy.get(`[data-test-id="delete-btn-${testDSID}"]`).click({force: true});
        });
    });
});