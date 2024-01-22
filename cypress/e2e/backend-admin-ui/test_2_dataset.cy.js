/// <reference types="cypress" />
import { visitAdminUI } from "../../integration-helpers/visitAdminUI";

context("Admin UI Single Dataset", () => {
  const testDSLabel = "TestDatasetName";
  const testDSID = "TestDatasetID2";

  describe("Create a new dataset", () => {
    beforeEach(() => {
      visitAdminUI("datasets");
    });

    it("Can create a new dataset", () => {
      cy.get('[data-test-id="entity-name"]').type(testDSLabel);
      cy.get('[data-test-id="entity-id"]').type(testDSID);
      cy.get('[data-test-id="create-dataset-btn"]').click().as("createDataset");
      cy.contains(testDSID);
    });
  });

  describe("Access on the created dataset", () => {
    beforeEach(() => {
      visitAdminUI(`datasets/${testDSID}`);
    });

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
    beforeEach(() => {
      visitAdminUI(`datasets/${testDSID}`);
    });

    it("Can upload table", () => {
      cy.intercept("/admin/datasets/*/tables").as("apiCall");
      cy.get('[data-test-id="upload-select"]').select("Table JSON");
      cy.get('[data-test-id="upload-input"]').selectFile(
        "./cypress/support/test_data/all_types.table.json"
      );
      cy.get('[data-test-id="upload-btn"]').click();
      cy.wait("@apiCall").reload();
      cy.get('[data-test-id="accordion-Tables"]').contains("td", `table`);
    });

    it("Can upload concept", () => {
      cy.intercept("/admin/datasets/*/concepts").as("apiCall");
      cy.get('[data-test-id="upload-select"]').select("Concept JSON");
      cy.get('[data-test-id="upload-input"]').selectFile(
        "./cypress/support/test_data/all_types.concept.json"
      );
      cy.get('[data-test-id="upload-btn"]').click();
      cy.wait("@apiCall").reload();
      cy.get('[data-test-id="accordion-Concepts"]').contains("td", `concept1`);
    });

    it("Can replace concept", () => {
      cy.intercept("/admin/datasets/*/concepts*").as("apiCall");
      cy.get('[data-test-id="upload-select"]').select("Concept JSON");
      cy.get('[data-test-id="upload-input"]').selectFile(
        "./cypress/support/test_data/all_types.concept.json"
      );
      cy.get('[data-test-id="upload-btn"]').click();
      cy.wait("@apiCall");
      cy.get(`[data-test-id="toast-custom-button"]`).click();
      cy.wait("@apiCall");
      cy.get('[data-test-id="toast"]').contains("The file has been posted successfully");
      cy.reload();
      cy.get('[data-test-id="accordion-Concepts"]').contains("td", `concept1`);
    });
  });

  describe("Can visit table info page", () => {
    beforeEach(() => {
      visitAdminUI(`datasets/${testDSID}/tables/${testDSID}.table`);
    });

    it("Can use page components", () => {
      cy.contains("Table table");
      cy.get('[data-test-id="accordion-Tags"]').click();
      cy.hash().should("eq", "#Tags");
      cy.get('[data-test-id="accordion-Concepts"]').click();
      cy.hash().should("eq", "#Concepts");
      cy.get('[data-test-id="accordion-Columns"]').click();
      cy.hash().should("eq", "#Columns");
    });
  });

  describe("Can visit concept info page", () => {
    beforeEach(() => {
      visitAdminUI(`datasets/${testDSID}/concepts/${testDSID}.concept1`);
    });

    it("Can use page components", () => {
      cy.contains("Concept Concept1");
      cy.get('[data-test-id="accordion-Selects"]').first().click();
      cy.hash().should("eq", "#Selects");
      cy.get('[data-test-id="accordion-Connectors"]').click();
      cy.hash().should("eq", "#Connectors");
    });
  });

  describe("Can visit connector info page", () => {
    it("Can use page components", () => {
      visitAdminUI(`datasets/${testDSID}/concepts/${testDSID}.concept1`);
      cy.get('[data-test-id="accordion-Connectors"]')
        .click()
        .get(".d-flex > :nth-child(1) > a")
        .click();

      cy.location("pathname").should(
        "equal",
        `/admin-ui/datasets/${testDSID}/connectors/${testDSID}.concept1.column`
      );
    });

    it("Counts are right", () => {
      visitAdminUI(`datasets/${testDSID}/connectors/${testDSID}.concept1.column`);
      cy.get('[data-test-id="accordion-Filters"] > .card-header').contains("20 entries");
      cy.get('[data-test-id="accordion-Selects"] > .card-header').contains("16 entries");
    });
  });

  describe("Connector page links work", () => {
    beforeEach(() => {
      visitAdminUI(`datasets/${testDSID}/connectors/${testDSID}.concept1.column`);
    });

    it("Goto datasets", () => {
      cy.get(".breadcrumb > :nth-child(1) > a").click();
      cy.location("pathname").should("equal", `/admin-ui/datasets`);
    });

    it("Goto dataset", () => {
      cy.get(".breadcrumb > :nth-child(2) > a").click();
      cy.location("pathname").should("equal", `/admin-ui/datasets/${testDSID}`);
    });

    it("Goto concepts", () => {
      cy.get(".breadcrumb > :nth-child(3) > a").click();
      cy.location("pathname")
        .should("equal", `/admin-ui/datasets/${testDSID}`)
        .location("hash")
        .should("equal", "#Concepts");
    });

    it("Goto concept", () => {
      cy.get(".breadcrumb > :nth-child(4) > a").click();
      cy.location("pathname").should(
        "equal",
        `/admin-ui/datasets/${testDSID}/concepts/${testDSID}.concept1`
      );
    });

    it("Goto connectors", () => {
      cy.get(".breadcrumb > :nth-child(5) > a").click();
      cy.location("pathname")
        .should("equal", `/admin-ui/datasets/${testDSID}/concepts/${testDSID}.concept1`)
        .location("hash")
        .should("equal", "#Connectors");
    });
  });

  describe("Can delete test concept and table", () => {
    beforeEach(() => visitAdminUI(`datasets/${testDSID}`));

    it("Can delete test concept", () => {
      cy.get('[data-test-id="accordion-Concepts"]').click();
      cy.get(`[data-test-id="delete-btn-concept-${testDSID}.concept1"]`).click({ force: true });
      cy.get(`[data-test-id="delete-btn-${testDSID}.concept1"]`).should("not.exist");
    });

    it("Can delete test table", () => {
      cy.get('[data-test-id="accordion-Tables"]').click();
      cy.get(`[data-test-id="delete-btn-table-${testDSID}.table"]`).click({ force: true });
      cy.get(`[data-test-id="delete-btn-table-${testDSID}.table"]`).should("not.exist");
    });

    it("Can force delete test table", () => {
      // reupload table and concept
      cy.get('[data-test-id="upload-select"]').select("Table JSON");
      cy.get('[data-test-id="upload-input"]').selectFile(
        "./cypress/support/test_data/all_types.table.json"
      );
      cy.get('[data-test-id="upload-btn"]').click();
      cy.reload();
      cy.get('[data-test-id="upload-select"]').select("Concept JSON");
      cy.get('[data-test-id="upload-input"]').selectFile(
        "./cypress/support/test_data/all_types.concept.json"
      );
      cy.get('[data-test-id="upload-btn"]').click();
      cy.reload();

      cy.get('[data-test-id="accordion-Tables"]').click();
      cy.get(`[data-test-id="delete-btn-table-${testDSID}.table"]`).click({ force: true });
      cy.get(`[data-test-id="toast-custom-button"]`).click({ force: true });
      cy.get(`[data-test-id="delete-btn-table-${testDSID}.table"]`).should("not.exist");
    });
  });

  describe("Delete the test dataset", () => {
    beforeEach(() => {
      visitAdminUI("datasets");
    });

    it("Can delete the test dataset", () => {
      cy.get(`[data-test-id="delete-btn-${testDSID}"]`).click({ force: true });
      cy.contains(testDSID).should("not.exist");
    });
  });
});
