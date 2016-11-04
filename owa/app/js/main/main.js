import angular from 'angular';
import uiRouter from 'angular-ui-router';
import mainComponent from './main.component.js';
import Home from '../home/home';
import Subscription from '../subscription/subscription';
import uicommons from 'openmrs-contrib-uicommons';
import Import from '../import/import';

let openconceptlabModule = angular.module('openconceptlab', [ uiRouter, Home.name, Subscription.name, Import.name, 'openmrs-contrib-uicommons'
    ])
    .component('main', mainComponent);

export default openconceptlabModule;
