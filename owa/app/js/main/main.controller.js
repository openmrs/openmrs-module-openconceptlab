class MainController {
    constructor(openmrsTranslate) {
        "ngInject"

        var vm = this;

        vm.changeLanguage = function (langKey) {
            return openmrsTranslate.changeLanguage(langKey);
        };
    }
}
export default MainController;