class HomeController {
  constructor($rootScope, $interval, $location, Upload, openmrsNotification, openmrsRest) {
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
    vm.isImporting = isImporting;
    vm.uploadZip = uploadZip;
    vm.isFileCorrect = isFileCorrect;

    /*
     * Upload button params
     */
    vm.endpoint = '/openmrs/ws/rest/v1/openconceptlab/import';
    vm.allowedFormat = 'application/zip, application/octet-stream';

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
      vm.isUploading = true;





      let upload = Upload.upload({
        url: vm.endpoint,
        data: {file: file},
      });

      upload.then(function(response) {
        openmrsNotification.success("File loaded successfully, importing...");
        handleStartImportSuccess(response);
      }, function(response) {
        openmrsNotification.error("Failed to load file");
        vm.isUploading = false;
      });
    }
    
    function getRunningImport() {
      openmrsRest.getFull("openconceptlab/import", {runningImport: true}).then(function (response) {
        vm.showLoading = false;
        vm.isUploading = false;
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
