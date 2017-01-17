class HomeController {
  constructor($rootScope, $interval, Upload, openmrsNotification, openmrsRest, ngDialog) {
    "ngInject"
    $rootScope.links = {};
    $rootScope.links["Open Concept Lab"] = "/";

    openmrsNotification.routeNotification();

    var vm = this;
    vm.showLoading = false;
    vm.textLength = 30;
    vm.updater = null;

    vm.startImportIfNoErrors = startImportIfNoErrors;
    vm.getRunningImport = getRunningImport;
    vm.setTextLength = setTextLength;
    vm.isImporting = isImporting;
    vm.uploadZip = uploadZip;
    vm.isFileCorrect = isFileCorrect;

    /*
     * Upload button params
     */
    vm.endpoint = '/openmrs/ws/rest/v1/openconceptlab/import';
    vm.allowedFormat = '.zip,application/zip,application/x-zip,application/x-zip-compressed,application/octet-stream';

    activate();
    
    function activate() {
      if(angular.isDefined(vm.runningImport)){
        vm.updater = $interval(function () {
          vm.getRunningImport();
        }, 2000);
      }
    }
    
    function isFileCorrect(fileName) {
      return fileName.endsWith(".zip");
    }

    function uploadZip(file) {
      vm.showLoading = true;

      let upload = Upload.upload({
        url: vm.endpoint,
        data: {file: file},
      });

      upload.then(function(response) {
        openmrsNotification.success("File loaded successfully, importing...");
        handleStartImportSuccess(response);
      }, function(response) {
        openmrsNotification.error("Failed to load file");
        vm.showLoading = false;
      });
    }

    function hasErrorsInPreviousImport() {
      if (angular.isDefined(vm.previousImports.results) && vm.previousImports.results.length != 0) {
        return vm.previousImports.results[0].errorItemsCount != 0;
      } else {
        return false;
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
    
    function isImporting() {
      return vm.runningImport != null;
    }

    function startImportIfNoErrors() {
      if(hasErrorsInPreviousImport()){
        ngDialog.openConfirm({
          template: 'importWarning.html',
          className: 'ngdialog-theme-default'
        }).then(function(value){
          if(value){
            ignoreErrors();
          } else {
            startImport();
          }
        });
      } else {
        startImport();
      }
    }

    function startImport(){
      ngDialog.close();
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

    function ignoreErrors() {
      let importAction = {
        anImport: vm.previousImports.results[0].uuid,
        ignoreAllErrors: true
      };
      openmrsRest.create("openconceptlab/importaction", importAction).then(handleIgnoreErrorsSuccess, handleIgnoreErrorsException);
      vm.showLoading = true;
    }

    function handleIgnoreErrorsSuccess(success) {
      startImport();
    }

    function handleIgnoreErrorsException(exception) {
      openmrsNotification.error(exception.data.error.message);
    }
  }
}

export default HomeController;
