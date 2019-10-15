(ns wheel.middleware.core
  (:require [clojure.spec.alpha :as s]
            [wheel.marketplace.channel :as channel]
            [wheel.infra.config :as config]
            [wheel.middleware.event :as event]
            [wheel.oms.message :as oms-message]))

(defmulti validate-message :type)

(defmulti validate-spec :type)

(defmulti parse :type)

(defn handle [{:keys [id]
               :as   oms-msg}]
  {:pre [(s/assert ::oms-message/oms-message oms-msg)]}

  (if-let [err (validate-message oms-msg)]
    [(event/parsing-failed id type err)]
    (try
      (let [parsed-oms-message (parse oms-msg)]
        (str "todo" parsed-oms-message))
      (catch Exception e
        (let [{:keys [error-message]} (ex-data e)]
          (event/parsing-failed id type error-message))))))

