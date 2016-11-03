import angular from 'angular';
import uiRouter from 'angular-ui-router';
import subscriptionComponent from './subscription.component.js';
import uicommons from 'openmrs-contrib-uicommons';

let subscriptionModule = angular.module('subscription', [ uiRouter, 'openmrs-contrib-uicommons'])
    .config(($stateProvider, $urlRouterProvider) => {
        "ngInject";
        $urlRouterProvider.otherwise('/');

        $stateProvider.state('subscription', {
            url: '/subscription',
            template: "<subscription subscription='$resolve.subscription'></subscription>",
            resolve: {
                subscription: getSubscription
            }
        })
    })
    .component('subscription', subscriptionComponent);

function getSubscription(openmrsRest) {
    return openmrsRest.getFull("openconceptlab/subscription").then(function (response) {
        return response.results[0];
    })
}

export default subscriptionModule;
