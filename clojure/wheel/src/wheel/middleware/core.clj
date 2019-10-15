(ns wheel.middleware.core
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [wheel.marketplace.channel :as channel]
            [wheel.infra.config :as config]
            [wheel.middleware.event :as event]
            [wheel.oms.message :as oms-message]
            [wheel.xsd :as xsd]))

(defmulti xsd-resource-file-path :type)

(defn- validate-message [oms-msg]
  (-> (xsd-resource-file-path oms-msg)
      io/resource
      io/as-file
      (xsd/validate (:message oms-msg))))

(defmulti parse :type)

(defn handle [{:keys [id type]
               :as   oms-msg}]
  {:pre [(s/assert ::oms-message/oms-message oms-msg)]}

  (if-let [err (validate-message oms-msg)]
    [(event/parsing-failed id type err)]
    (try
      (let [parsed-oms-message (parse oms-msg)]
        (str "todo" (vec parsed-oms-message)))
      (catch Exception e
        (let [{:keys [error-message]} (ex-data e)]
          (event/parsing-failed id type error-message))))))

(comment
  (def ranging-sample
    "
<EXTNChannelList>
  <EXTNChannelItemList>
    <EXTNChannelItem ChannelID=\"UA\" EAN=\"UA_EAN_1\" 
                     ItemID=\" \" RangeFlag=\"Y\"/>
  </EXTNChannelItemList>
  <EXTNChannelItemList>
    <EXTNChannelItem ChannelID=\"UB\" EAN=\"UB_EAN_3\" 
                     ItemID=\"SKU3\" RangeFlag=\"Y\"/>
  </EXTNChannelItemList>
</EXTNChannelList>
    ")
  (handle {:id      (java.util.UUID/randomUUID)
           :message ranging-sample
           :type    :ranging}))

