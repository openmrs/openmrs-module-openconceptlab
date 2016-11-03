import template from './home.html';
import controller from './home.controller.js';

let homeComponent = {
    restrict: 'E',
    bindings: {
        subscription : "<",
        runningImport : "<",
        previousImports : "<"
    },
    template: template,
    controller: controller,
    controllerAs: 'vm'
};

export default homeComponent;
