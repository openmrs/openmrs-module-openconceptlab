class SubscriptionController {
  constructor($rootScope, $location, openmrsRest, openmrsNotification) {
    "ngInject"
    $rootScope.links = {};
    $rootScope.links["Open Concept Lab"] = "";
    $rootScope.links["Subscription"] = "subscription";

    const NUMBER_OF_SLASHES_AFTER_BASE_URL = 5;
    var vm = this;
    vm.cancel = cancel;
    vm.subscribe = subscribe;
    vm.unSubscribe = unSubscribe;
    vm.isVersionAdded = isVersionAdded;

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

    /*
    *This checks if collection version has been passed to subscription url by checking number of forward slashes after base url
    *If the number is 5, such as with https://api.openconceptlab.org/users/username/collections/collectionname/v1.0
    *that means collection version was passed and isVersionAdded() will return true
    */
    function isVersionAdded(subscriptionUrl) {
    if (subscriptionUrl.endsWith("/")) {
            subscriptionUrl = subscriptionUrl.substring(0, subscriptionUrl.lastIndexOf('/'));
    }
    var url = new URL(subscriptionUrl);
    let subUrlAfterBaseUrl = url.pathname;

      let count = subUrlAfterBaseUrl.length - subUrlAfterBaseUrl.replace(/[\/\\]/g, '').length;
      if (count == NUMBER_OF_SLASHES_AFTER_BASE_URL) {
        vm.subscription.subscribedToSnapshot = false;
        return true;
      } else {
        return false
      }
    }
  }
}

export default SubscriptionController;
