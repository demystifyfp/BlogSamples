(ns wheel.middleware.ranging
  (:require [clojure.xml :as xml]
            [clojure.spec.alpha :as s]
            [wheel.oms.item :as oms-item]
            [wheel.marketplace.channel :as channel])
  (:import [java.io StringBufferInputStream]))

(s/def ::item
  (s/keys :req-un [::oms-item/ean ::oms-item/id]))

(s/def ::items (s/coll-of ::item :min-count 1))

(s/def ::channel-id ::channel/id)
(s/def ::channel-items
  (s/keys :req-un [::channel-id ::items]))

(s/def ::message
  (s/coll-of ::channel-items :min-count 1))

(defn- to-channel-item [{:keys [EAN ItemID]}]
  {:ean EAN
   :id ItemID})

(defn- parse-message [message]
  {:post [(s/assert ::message %)]}
  (->> (StringBufferInputStream. message)
       xml/parse
       :content
       (mapcat :content)
       (map :attrs)
       (group-by :ChannelID)
       (map (fn [[id xs]]
              {:channel-id  id
               :items (map to-channel-item xs)}))))

(comment
  (s/check-asserts true)
  (let [msg "
<EXTNChannelList>
  <EXTNChannelItemList>
    <EXTNChannelItem ChannelID=\"UA\" EAN=\"UA_EAN_1\" 
                     ItemID=\"SKU1\" RangeFlag=\"Y\"/>
    <EXTNChannelItem ChannelID= \"UA\" EAN= \"UA_EAN_2 \"
ItemID= \"SKU2\" RangeFlag= \"Y \"/>      
  </EXTNChannelItemList>
  <EXTNChannelItemList>
    <EXTNChannelItem ChannelID=\"UB\" EAN=\"UB_EAN_3\" 
                     ItemID=\"SKU3\" RangeFlag=\"Y\"/>
  </EXTNChannelItemList>
</EXTNChannelList>
    "]
    (parse-message msg)))