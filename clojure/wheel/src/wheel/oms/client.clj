(ns wheel.oms.client
  (:require [wheel.oms.order :as oms-order]
            [wheel.infra.oms :as oms-infra]
            [clojure.spec.alpha :as s]))

(defn- send [session producer xml-message]
  (let [msg (.createTextMessage session)]
    (.setText msg xml-message)
    (.send producer msg)))

(defn allocate-order [order]
  {:pre [(s/assert ::oms-order/order order)]}
  (send oms-infra/order-allocating-session
        oms-infra/order-allocating-producer
        (oms-order/to-xml order)))
