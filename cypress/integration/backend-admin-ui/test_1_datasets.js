/// <reference types="cypress" />
import { visitAdminUI } from "../../integration-helpers/visitAdminUI";

context("Admin UI Datasets", () => {
    const testDSLabel = "TestDatasetName";
    const testDSID = "TestDatasetID";

    describe("Access on the page", () => {
        before(() => { visitAdminUI('datasets'); });

        it("Can see the datasets page", () => {
            cy.contains("Datasets");
        });
    });

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

    describe("Delete a dataset", () => {
        before(() => { visitAdminUI('datasets'); });

        it("Can delete the test dataset", () => {
            cy.get(`[data-test-id="delete-btn-${testDSID}"]`).click({force: true});
            cy.get(`[data-test-id="delete-btn-${testDSID}"]`).should('not.exist');
        });
    });
});