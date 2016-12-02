class SubscriptionController {
  constructor($rootScope, $location, openmrsRest, openmrsNotification) {
    "ngInject"
    $rootScope.links = {};
    $rootScope.links["Open Concept Lab"] = "";
    $rootScope.links["Subscription"] = "subscription";

    var vm = this;
    vm.cancel = cancel;
    vm.subscribe = subscribe;
    vm.unSubscribe = unSubscribe;

    function cancel(){
      $location.path('/');
    }

    function unSubscribe() {
      openmrsRest.retire("openconceptlab/subscription", vm.subscription).then(handleUnSubscribeSuccess, handleUnSubscribeException);
    }

    function handleUnSubscribeSuccess(success) {
      openmrsNotification.success("Unsubscribed successfully");
      getSubscription();
    }

    function handleUnSubscribeException(exception) {
      openmrsNotification.error(exception.data.error.message);
    }

    function subscribe() {
      if (angular.isUndefined(vm.subscription.uuid) || vm.subscription.uuid === "") {
        openmrsRest.create("openconceptlab/subscription", vm.subscription).then(handleSubscribeSuccess, handleSubscribeException);
      } else {
        openmrsRest.update("openconceptlab/subscription", vm.subscription).then(handleSubscribeSuccess, handleSubscribeException);
      }
    }

    function handleSubscribeSuccess(success) {
      $location.path('/').search({successToast: "Subscription saved"});
    }

    function handleSubscribeException(exception) {
      openmrsNotification.error(exception.data.error.message);
    }

    function getSubscription() {
      openmrsRest.getFull("openconceptlab/subscription").then(function (response) {
        vm.subscription = response.results[0];
      })
    }
  }
}

export default SubscriptionController;
