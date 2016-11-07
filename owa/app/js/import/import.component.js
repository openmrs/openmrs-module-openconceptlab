import template from './import.html';
import controller from './import.controller.js';

let importComponent = {
    restrict: 'E',
    bindings: {
        anImport : "<",
        items : "<"
    },
    template: template,
    controller: controller,
    controllerAs: 'vm'
};

export default importComponent;
