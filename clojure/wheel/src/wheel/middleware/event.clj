(ns wheel.middleware.event
  (:require [clojure.spec.alpha :as s]
            [wheel.marketplace.channel :as channel]
            [wheel.offset-date-time :as offset-date-time]))

(s/def ::id uuid?)
(s/def ::parent-id ::id)
(s/def ::name qualified-keyword?)
(s/def ::level #{:info :warn :debug :error :fatal})
(s/def ::timestamp ::offset-date-time/ist-timestamp)
(s/def ::type #{:domain :system :oms})

(s/def ::channel-id ::channel/id)
(s/def ::channel-name ::channel/name)

(defmulti event-type :type)
(defmethod event-type :system [_]
  (s/keys :req-un [::id ::name ::type ::level ::timestamp]
          :opt-un [::parent-id]))
(defmethod event-type :domain [_]
  (s/keys :req-un [::id ::name ::type ::level ::timestamp 
                   ::channel-id ::channel-name]
          :opt-un [::parent-id]))
(defmethod event-type :default [_]
  (s/keys :req-un [::type]))
(defmethod event-type :oms [_]
  (s/keys :req-un [::id ::name ::type ::level ::timestamp]))

(s/def ::event (s/multi-spec event-type :type))

(defn domain? [event]
  (and (s/valid? ::event event) (= :domain (:type event))))

(comment
  (s/valid?
   ::event
   {:name :ranging/succeeded
    :type :domain
    :channel-id 1
    :level :info
    :timestamp "2019-10-01T12:30+05:30"
    :id (java.util.UUID/randomUUID)
    :channel-name :tata-cliq})
  (s/valid?
   ::event
   {:name :db.migration/failed
    :type :system
    :level :fatal
    :timestamp "2019-10-01T12:30+05:30"
    :id (java.util.UUID/randomUUID)}))