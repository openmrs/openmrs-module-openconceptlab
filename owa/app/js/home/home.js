import angular from 'angular';
import uiRouter from 'angular-ui-router';
import homeComponent from './home.component.js';
import uicommons from 'openmrs-contrib-uicommons';
import ngFileUpload from 'ng-file-upload';

let homeModule = angular.module('home', [ uiRouter, 'openmrs-contrib-uicommons', 'ngFileUpload'])
    .config(($stateProvider, $urlRouterProvider) => {
        "ngInject";
        $urlRouterProvider.otherwise('/');

        $stateProvider.state('home', {
            url: '/',
            template: "<home subscription='$resolve.subscription' running-import='$resolve.getRunningImport' previous-imports='$resolve.getImports'></home>",
            resolve: {
                subscription: subscription,
                getRunningImport: getRunningImport,
                getImports: getImports
            }
        })
    })
    .component('home', homeComponent);

function getRunningImport(openmrsRest) {
    return openmrsRest.getFull("openconceptlab/import", {runningImport: true}).then(function (response) {
        return response.results[0];
    })
}

function subscription(openmrsRest) {
    return openmrsRest.getFull("openconceptlab/subscription").then(function (response) {
        return response.results[0];
    })
}

function getImports(openmrsRest) {
    return openmrsRest.getFull("openconceptlab/import").then(function (response) {
        return response;
    })
}

export default homeModule;
