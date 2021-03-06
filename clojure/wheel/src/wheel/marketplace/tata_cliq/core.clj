(ns wheel.marketplace.tata-cliq.core
  (:require [wheel.marketplace.tata-cliq.api :as tata-cliq]
            [wheel.marketplace.tata-cliq.order :as tata-cliq-order]
            [wheel.oms.client :as oms]
            [wheel.marketplace.channel :as channel]
            [wheel.middleware.ranging :as ranging]
            [wheel.oms.message :as oms-message]
            [wheel.middleware.event :as event]
            [clojure.spec.alpha :as s]
            [clojure.set :as set]))

(defmethod ranging/process-ranging :tata-cliq
  [{:keys [channel-name]
    :as   channel-config}
   {:keys [id]
    :as   oms-msg}
   {:keys [channel-id items]
    :as   channel-items}]
  {:pre [(s/assert ::oms-message/oms-message oms-msg)
         (s/assert ::ranging/channel-items channel-items)]}
  (try
    (tata-cliq/ranging channel-config channel-id
                       (map #(set/rename-keys % {:id :sku}) items))
    (event/ranging-succeeded id channel-id channel-name items)
    (catch Throwable ex
      (event/processing-failed ex id :ranging channel-id channel-name))))

(defmethod channel/allocate-order :tata-cliq [channel-id channel-config]
  (->> (tata-cliq/new-orders channel-config channel-id)
       (map tata-cliq-order/to-oms-order)
       (run! oms/allocate-order)))