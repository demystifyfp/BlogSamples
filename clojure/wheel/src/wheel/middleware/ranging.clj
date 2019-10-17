(ns wheel.middleware.ranging
  (:require [clojure.xml :as xml]
            [clojure.spec.alpha :as s]
            [wheel.oms.item :as oms-item]
            [wheel.middleware.event :as event]
            [wheel.infra.config :as config]
            [wheel.marketplace.channel :as channel]
            [wheel.middleware.core :as middleware])
  (:import [java.io StringBufferInputStream]))

(s/def ::item
  (s/keys :req-un [::oms-item/ean ::oms-item/id]))

(s/def ::items (s/coll-of ::item :min-count 1))

(s/def ::channel-id ::channel/id)
(s/def ::channel-items
  (s/keys :req-un [::channel-id ::items]))

(s/def ::message
  (s/coll-of ::channel-items :min-count 1))

(defn- to-item [{:keys [EAN ItemID]}]
  {:ean EAN
   :id ItemID})

(defmethod middleware/parse :ranging [{:keys [message]}]
  (->> (StringBufferInputStream. message)
    xml/parse
    :content
    (mapcat :content)
    (map :attrs)
    (group-by :ChannelID)
    (map (fn [[id xs]]
           {:channel-id id
            :items      (map to-item xs)}))))

(defmethod middleware/xsd-resource-file-path :ranging [_]
  "oms/message_schema/ranging.xsd")

(defmethod middleware/spec :ranging [_]
  ::message)

(defmulti process-ranging (fn [{:keys [channel-name]} oms-msg ranging-message]
                            channel-name))

(defmethod middleware/process :ranging [{:keys [id]
                                         :as oms-msg} ranging-message]
  (for [{:keys [channel-id]
         :as   ch-ranging-message} ranging-message]
    (if-let [channel-config (config/get-channel-cofig channel-id)]
      (try
        (process-ranging channel-config oms-msg ch-ranging-message)
        (catch Throwable ex
          (event/processing-failed ex id :ranging channel-id (:channel-name channel-config))))
      (event/channel-not-found id :ranging channel-id))))

(comment
  (s/check-asserts true)
  (let [msg "
<EXTNChannelList>
  <EXTNChannelItemList>
    <EXTNChannelItem ChannelID=\"UA\" EAN=\"UA_EAN_1\" 
                     ItemID=\"SKU1\" RangeFlag=\"Y\"/>
    <EXTNChannelItem ChannelID= \"UA\" EAN= \"UA_EAN_2 \"
ItemID= \"SKU2\" RangeFlag= \"Y\"/>      
  </EXTNChannelItemList>
  <EXTNChannelItemList>
    <EXTNChannelItem ChannelID=\"UB\" EAN=\"UB_EAN_3\" 
                     ItemID=\"SKU3\" RangeFlag=\"Y\"/>
  </EXTNChannelItemList>
</EXTNChannelList>
    "]
    (parse-message msg)))