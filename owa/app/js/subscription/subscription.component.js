import template from './subscription.html';
import controller from './subscription.controller.js';

let subscriptionComponent = {
    restrict: 'E',
    bindings: {
        subscription : "<"
    },
    template: template,
    controller: controller,
    controllerAs: 'vm'
};

export default subscriptionComponent;
