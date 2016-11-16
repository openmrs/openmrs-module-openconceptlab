import angular from 'angular';
import uiRouter from 'angular-ui-router';
import importComponent from './import.component.js';
import uicommons from 'openmrs-contrib-uicommons';

let importModule = angular.module('import', [ uiRouter, 'openmrs-contrib-uicommons'])
    .config(($stateProvider, $urlRouterProvider) => {
        "ngInject";
        $urlRouterProvider.otherwise('/');

        $stateProvider.state('import', {
            url: '/import/:UUID',
            template: "<import an-import='$resolve.getImport' items='$resolve.getItems'></import>",
            resolve: {
                getImport: getImport,
                getItems: getItems
            }
        })
    })
    .component('import', importComponent);

function getImport(openmrsRest, $stateParams) {
    return openmrsRest.getFull("openconceptlab/import", {uuid: $stateParams.UUID}).then(function (response) {
        return response;
    })
}

function getItems(openmrsRest, $stateParams) {
    return openmrsRest.getFull("openconceptlab/import/"+$stateParams.UUID+"/item", {state: "ERROR"}).then(function (response) {
        return response.results;
    })
}

export default importModule;
