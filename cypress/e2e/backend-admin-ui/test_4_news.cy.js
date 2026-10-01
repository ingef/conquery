/// <reference types="cypress" />
import { visitAdminUI } from "../../integration-helpers/visitAdminUI";

context("Admin UI News", () => {
  it("Creates, orders, and deletes news with suggested and custom categories", () => {
    const suffix = Date.now();
    const datasetId = `NewsDataset${suffix}`;
    const customNewsId = `custom-news-${suffix}`;
    const olderTitle = `Older Cypress News ${suffix}`;
    const olderNewsId = olderTitle.toLowerCase().replaceAll(" ", "-");

    visitAdminUI("datasets");
    cy.get('[data-test-id="entity-name"]').type(`News Dataset ${suffix}`);
    cy.get('[data-test-id="entity-id"]').type(datasetId);
    cy.get('[data-test-id="create-dataset-btn"]').click();
    cy.contains(datasetId);

    visitAdminUI("news");
    cy.contains("h1", "News");
    cy.get('[data-test-id="news-date"]').should("have.value", localDateString(new Date()));

    cy.get('[data-test-id="news-title"]').type("Generated News");
    cy.get('[data-test-id="news-id"]').should("have.value", "generated-news");
    cy.get('[data-test-id="news-id"]').clear().type(customNewsId);
    cy.get('[data-test-id="news-title"]').type(" Updated");
    cy.get('[data-test-id="news-id"]').should("have.value", customNewsId);
    cy.get('[data-test-id="news-description"]').type("First line{enter}Second line");
    cy.get('[data-test-id="news-link"]').type("https://example.com/news");

    cy.get('[data-test-id="news-category-input"]').focus();
    cy.contains('[data-test-id="news-category-suggestion"]', datasetId).click();
    cy.get('[data-test-id="news-category-input"]').type("custom-category{enter}");
    cy.get('[data-test-id="news-category-token"]').should("have.length", 2);
    cy.get('[data-test-id="create-news-btn"]').click();

    cy.get(`[data-test-id="news-row-${customNewsId}"]`)
      .should("contain.text", "Generated News Updated")
      .and("contain.text", datasetId)
      .and("contain.text", "custom-category");

    cy.get('[data-test-id="news-title"]').type(olderTitle);
    cy.get('[data-test-id="news-id"]').should("have.value", olderNewsId);
    cy.get('[data-test-id="news-description"]').type("Older news item");
    cy.get('[data-test-id="news-date"]').type("2000-01-01");
    cy.get('[data-test-id="create-news-btn"]').click();
    cy.get(`[data-test-id="news-row-${olderNewsId}"]`).should("exist");

    cy.get(`[data-test-id="news-row-${customNewsId}"]`).then(($newerRow) => {
      cy.get(`[data-test-id="news-row-${olderNewsId}"]`).then(($olderRow) => {
        expect($newerRow.index()).to.be.lessThan($olderRow.index());
      });
    });

    cy.on("window:confirm", () => true);
    cy.get(`[data-test-id="delete-news-${olderNewsId}"]`).click();
    cy.get(`[data-test-id="news-row-${olderNewsId}"]`).should("not.exist");
    cy.get(`[data-test-id="delete-news-${customNewsId}"]`).click();
    cy.get(`[data-test-id="news-row-${customNewsId}"]`).should("not.exist");

    visitAdminUI("datasets");
    cy.get(`[data-test-id="delete-btn-${datasetId}"]`).click({ force: true });
    cy.get(`[data-test-id="delete-btn-${datasetId}"]`).should("not.exist");
  });
});

function localDateString(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
