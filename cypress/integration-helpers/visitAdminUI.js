export const visitAdminUI = (page = '') => {
    cy.visit('http://localhost:8081/admin-ui/' + page);
};
  