export const visitWithToken = (token) => {
  cy.intercept({ method: "GET", url: "/api/config/frontend" }).as("config");
  cy.intercept({ method: "GET", url: "/api/datasets" }).as("datasets");
  cy.intercept({ method: "GET", url: "/api/me" }).as("meInfo");

  cy.visit(`http://localhost:8000/?access_token=${token}`);

  cy.wait(["@config", "@datasets", "@meInfo"]);
};
