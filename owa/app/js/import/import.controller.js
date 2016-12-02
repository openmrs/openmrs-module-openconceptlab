class ImportController {
  constructor($rootScope) {
    "ngInject"
    $rootScope.links = {};
    $rootScope.links["Open Concept Lab"] = "";
    $rootScope.links["Import"] = "import";

    var vm = this;

  }
}

export default ImportController;
