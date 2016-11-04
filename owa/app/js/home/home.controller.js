class HomeController {
  constructor($rootScope, $interval, openmrsNotification, openmrsRest) {
    "ngInject"
    $rootScope.links = {};
    $rootScope.links["Open Concept Lab"] = "/";

    var vm = this;
    vm.showLoading = false;
    vm.textLength = 30;
    vm.updater = null;

    vm.startImport = startImport;
    vm.getRunningImport = getRunningImport;
    vm.setTextLength = setTextLength;

    activate();
    
    function activate() {
      if(angular.isDefined(vm.runningImport)){
        vm.updater = $interval(function () {
          vm.getRunningImport();
        }, 2000);
      }
    }
    
    function getRunningImport() {
      openmrsRest.getFull("openconceptlab/import", {runningImport: true}).then(function (response) {
        vm.showLoading = false;
        vm.runningImport = response.results[0];
        if(angular.isDefined(vm.runningImport) && vm.runningImport.importProgress === 100){
          if(angular.isDefined(vm.updater)){
            $interval.cancel(vm.updater);
            vm.runningImport = null;
            getPreviousImports();
          }
        } else if(!angular.isDefined(vm.runningImport)){
          $interval.cancel(vm.updater);
          vm.runningImport = null;
          getPreviousImports();
        }
      })
    }

    function startImport() {
      let anImport = {};
      openmrsRest.create("openconceptlab/import", anImport).then(handleStartImportSuccess, handleStartImportException);
      vm.showLoading = true;
    }

    function handleStartImportSuccess(success) {
      vm.updater = vm.updater = $interval(function () {
        vm.getRunningImport();
      }, 2000);
    }
    function handleStartImportException(exception) {
      vm.showLoading = false;
      openmrsNotification.error(exception.data.error.message);
    }

    function setTextLength(length) {
      vm.textLength = length;
    }

    function getPreviousImports() {
      openmrsRest.getFull("openconceptlab/import").then(onGetImportSuccess, onGetImportException);
    }

    function onGetImportSuccess(response) {
      vm.previousImports = response;
    }

    function onGetImportException(exception) {
      openmrsNotification.error(exception.data.error.message);
    }
  }
}

export default HomeController;
