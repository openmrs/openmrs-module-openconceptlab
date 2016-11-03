class SubscriptionController {
  constructor($rootScope, $location, $window, openmrsRest, openmrsNotification) {
    "ngInject"
    $rootScope.links = {};
    $rootScope.links["Open Concept Lab"] = "/";
    $rootScope.links["Subscription"] = "subscription";

    var vm = this;
    vm.cancel = cancel;
    vm.subscribe = subscribe;
    vm.unSubscribe = unSubscribe;
    
    function activate() {
      if(!angular.isDefined(vm.subscription)){
        vm.subscription = {
          url: "",
          token: ""
        }
      }
    }

    function cancel(){
      $location.path('/');
    }

    function unSubscribe() {
      openmrsRest.retire("openconceptlab/subscription", vm.subscription).then(handleUnSubscribeSuccess, handleUnSubscribeException);
    }

    function handleUnSubscribeSuccess(success) {
      $window.location.reload();
    }

    function handleUnSubscribeException(exception) {
      openmrsNotification.error(exception.data.error.message);
    }

    function subscribe() {
      openmrsRest.create("openconceptlab/subscription", vm.subscription).then(handleSubscribeSuccess, handleSubscribeException);
    }

    function handleSubscribeSuccess(success) {
      $location.path('/').search({successToast: "Subscription saved"});
    }

    function handleSubscribeException(exception) {
      openmrsNotification.error(exception.data.error.message);
    }
  }
}

export default SubscriptionController;
