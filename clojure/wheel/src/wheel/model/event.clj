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
         {:name :keyword
          :channel-name :channel-name
          :level :event-level}))

(defn- timestamp->offset-date-time [timestamp]
  (OffsetDateTime/parse timestamp DateTimeFormatter/ISO_OFFSET_DATE_TIME))

(defn create! [new-event]
  {:pre [(s/assert ::event/event new-event)
         (s/assert event/domain? new-event)]}
  (as-> new-event evt
    (update evt :timestamp timestamp->offset-date-time)
    (dissoc evt :type)
    (db/insert! Event evt)))

(comment
 (create! {:name :ranging/succeeded
           :type :domain
           :channel-id "UA"
           :level :info
           :timestamp "2019-10-01T12:30+05:30"
           :id (java.util.UUID/randomUUID)
           :channel-name :tata-cliq}))