export const adminBaseUrl = 'http://localhost:8081'

export const visitAdminUI = (page = '') => {
    cy.visit(adminBaseUrl + '/admin-ui/' + page);
};

export const checkFreemarkerError = () => {
    cy.root().should('not.contain.text', 'FreeMarker template error')
}
  