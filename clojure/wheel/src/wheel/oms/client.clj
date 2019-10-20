(ns wheel.oms.client
  (:require [wheel.oms.order :as oms-order]
            [clojure.spec.alpha :as s]))

(defn allocate-order [order]
  {:pre [(s/assert ::oms-order/order order)]}
  (prn (oms-order/to-xml order)))
