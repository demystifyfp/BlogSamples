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
  {:pre [(s/assert event/domain-or-oms? new-event)]}
  (db/insert! Event
              (update new-event :timestamp timestamp->offset-date-time)))

(comment
  (create! {:name         :ranging/succeeded
            :type         :domain
            :channel-id   "UA"
            :level        :info
            :timestamp    "2019-10-01T12:30+05:30"
            :id           (java.util.UUID/randomUUID)
            :channel-name :tata-cliq
            :payload      {:type :ranging/succeeded
                           :a    1}}))