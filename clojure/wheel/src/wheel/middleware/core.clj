(ns wheel.middleware.core
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [wheel.middleware.event :as event]
            [wheel.oms.message :as oms-message]
            [wheel.xsd :as xsd]))

(defmulti xsd-resource-file-path :type)
(defmulti parse :type)
(defmulti spec :type)
(defmulti process (fn [oms-msg parsed-oms-message]
                    (:type oms-msg)))

(defn- validate-message [oms-msg]
  (-> (xsd-resource-file-path oms-msg)
      io/resource
      io/as-file
      (xsd/validate (:message oms-msg))))

(defn handle [{:keys [id type]
               :as   oms-msg}]
  {:pre  [(s/assert ::oms-message/oms-message oms-msg)]
   :post [(s/assert (s/coll-of ::event/event :min-count 1) %)]}
  (if-let [err (validate-message oms-msg)]
    [(event/parsing-failed id type err)]
    (let [parsed-oms-message (parse oms-msg)]
      (if (s/valid? (spec oms-msg) parsed-oms-message)
        (process oms-msg parsed-oms-message)
        [(event/parsing-failed
          id type
          (s/explain-str (spec oms-msg) parsed-oms-message))]))))

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

