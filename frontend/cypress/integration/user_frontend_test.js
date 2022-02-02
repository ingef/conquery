/// <reference types="cypress" />

context('Visit conquery as users with different permissions', () => {
    describe('Visit as a authenticated user without permissions', () => {
        before(() => {        
            cy.intercept({
                method: 'GET',
                url: 'http://localhost:8080/api/config/frontend',
            }
            ).as('config')
            cy.intercept({
                method: 'GET',
                url: 'http://localhost:8080/api/datasets',
            }
            ).as('datasets')
            cy.intercept({
                method: 'GET',
                url: 'http://localhost:8080/api/me',
            }
            ).as('meInfo')
    
            cy.visit("http://localhost:3000/?access_token=user.user1")
    
            cy.wait(['@config', '@datasets', '@meInfo'])
        })

        it('Cannot see concepts', () => {
            cy.contains("Keine Konzepte")
        })


        it('Cannot see queries', () => {

            cy.get('.Pane1').contains('Anfragen').click()


            cy.contains("Keine Anfragen gefunden")
        })

        
        it('Cannot see forms', () => {

            cy.get('.Pane1').contains('Formulare').click()


            cy.contains("Keine Formular-Konfigurationen gefunden")
        })
    })

    describe('Visit as a authenticated user with permissions', () => {
        before(() => {        
            cy.intercept({
                method: 'GET',
                url: 'http://localhost:8080/api/config/frontend',
            }
            ).as('config')
            cy.intercept({
                method: 'GET',
                url: 'http://localhost:8080/api/datasets',
            }
            ).as('datasets')
            cy.intercept({
                method: 'GET',
                url: 'http://localhost:8080/api/me',
            }
            ).as('meInfo')
    
            cy.visit("http://localhost:3000/?access_token=user.user2")
    
            cy.wait(['@config', '@datasets', '@meInfo'])
        })

        
        it('Can see dataset', () => {
            cy.get('#downshift-0-input').invoke('attr','value').should('eq',"Dataset1")
        })

        it('Can see concepts', () => {
            cy.contains("Concept1")
        })
    })
})