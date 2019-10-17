(ns wheel.model.event
  (:require [toucan.models :as models]
            [clojure.spec.alpha :as s]
            [toucan.db :as db]
            [wheel.middleware.event :as event])
  (:import [java.time OffsetDateTime]
           [java.time.format DateTimeFormatter]))

(models/defmodel Event :event
  models/IModel
  (types [_]
         {:name         :keyword
          :channel-name :channel-name
          :level        :event-level
          :type         :event-type
          :payload      :jsonb}))

(defn- timestamp->offset-date-time [timestamp]
  (OffsetDateTime/parse timestamp DateTimeFormatter/ISO_OFFSET_DATE_TIME))

(defn create! [new-event]
  {:pre [(s/assert ::event/event new-event)]}
  (try
    (db/insert! Event
                (update new-event :timestamp timestamp->offset-date-time))
    (catch Throwable ex
      (prn "<<>>>" ex))))

(comment
  (db/insert! Event {:id        #uuid "a3c18c62-2795-4511-859d-58f56b006f91"
                     :timestamp "2019-10-18T00:48:46.354+05:30"
                     :name      :oms/items-ranged
                     :level     :info
                     :type      :oms
                     :payload   {:message "<EXTNChannelList>   <EXTNChannelItemList>     <EXTNChannelItem ChannelID=\"UA\" EAN=\"EAN_1\" ItemID=\"SKU1\" RangeFlag=\"Y\"/>   </EXTNChannelItemList> </EXTNChannelList>"
                                 :type    :oms/items-ranged}})
  (create! {:name         :ranging/succeeded
            :type         :domain
            :channel-id   "UA"
            :level        :info
            :timestamp    "2019-10-01T12:30+05:30"
            :id           (java.util.UUID/randomUUID)
            :channel-name :tata-cliq}))