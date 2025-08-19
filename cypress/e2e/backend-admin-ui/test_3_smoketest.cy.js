/// <reference types="cypress" />
import {adminBaseUrl, checkFreemarkerError, visitAdminUI} from "../../integration-helpers/visitAdminUI";

context("Simple UI Render Smoke Tests", () => {

    describe("Query Overview renders", () => {

        it("Visit Query Overview", () => {

            visitAdminUI("queries");

            // Disable updates so the test ends eventually
            cy.get('#updateCheckBox').click()
            checkFreemarkerError()
        });
    });

    describe("Query Jobs renders", () => {

        it("Visit Jobs Overview", () => {

            visitAdminUI("jobs");

            checkFreemarkerError()
        });
    });

    describe("Groups renders", () => {

        it("Post faulty group (member id not resolvable)", () => {

            cy.request({
                method: 'POST',
                url: adminBaseUrl + "/admin/groups/",
                body: [{
                    name: "faulty_group",
                    label: "Faulty Group",
                    members: [
                        "user.unresolvable"
                    ],
                    roles: [
                        "role.unresolvable"
                    ]
                }]
            });
        });


        it("Visit Groups", () => {

            visitAdminUI("groups");

            checkFreemarkerError();
        });

        it("Visit Faulty Group", () => {
            visitAdminUI("groups/group.faulty_group");

            checkFreemarkerError();

            cy.get('#member > .table-responsive').contains('Unresolvable Member')

            cy.get('#roles > .table-responsive').contains('Unresolvable Role')
        })


        it("Delete faulty member and role", () => {
            visitAdminUI("groups/group.faulty_group");

            cy.get('#members-tab').click()
            cy.get('#member > .table-responsive > .table > tbody > tr > :nth-child(2) > a').click()

            cy.get('#roles-tab').click()
            cy.get('#roles > .table-responsive > .table > tbody > tr > :nth-child(2) > a').click()

            checkFreemarkerError();

            cy.get('#member > .table-responsive').should('not.contain.text', 'Unresolvable Member')

            cy.get('#roles > .table-responsive').should('not.contain.text', 'Unresolvable Role')
        })
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

            cy.get('[data-test-id="accordion-Columns"]').click()

            cy.root().should('not.contain.text', 'FreeMarker template error')


        });
    });
})