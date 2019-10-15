(ns wheel.middleware.event
  (:require [clojure.spec.alpha :as s]
            [wheel.marketplace.channel :as channel]
            [wheel.offset-date-time :as offset-date-time]
            [wheel.oms.item :as item]
            [wheel.oms.message :as oms-message])
  (:import [java.util UUID]))

(s/def ::id uuid?)
(s/def ::parent-id ::id)

(s/def ::oms-event-name #{:oms/items-ranged})
(s/def ::domain-event-name #{:ranging/succeeded :ranging/failed})
(s/def ::system-event-name #{:system/parsing-failed
                             :system/channel-not-found})

(s/def ::name (s/or :oms ::oms-event-name 
                    :domain ::domain-event-name
                    :system ::system-event-name))

(s/def ::level #{:info :warn :debug :error :fatal})
(s/def ::timestamp ::offset-date-time/ist-timestamp)
(s/def ::type #{:domain :system :oms})

(s/def ::channel-id ::channel/id)
(s/def ::channel-name ::channel/name)


(defmulti payload-type :type)

(s/def ::oms-message string?)
(defmethod payload-type :oms/items-ranged [_]
  (s/keys :req-un [::oms-message]))

(s/def ::item-ids (s/coll-of ::item/id :min-count 1))
(defmethod payload-type :ranging/succeeded [_] 
  (s/keys :req-un [::item-ids]))

(s/def ::error-message (s/and string? (complement clojure.string/blank?)))
(s/def ::stacktrace (s/and string? (complement clojure.string/blank?)))
(defmethod payload-type :ranging/failed [_]
  (s/keys :req-un [::error-message ::stacktrace]))

(defmethod payload-type :system/parsing-failed [_]
  (s/keys :req-un [::error-message]))
(defmethod payload-type :system/channel-not-found [_]
  (s/keys :req-un [::channel-id]))

(defmethod payload-type :default [_]
  (s/keys :req-un [::type]))
(s/def ::payload (s/multi-spec payload-type :type))

(defmulti event-type :type)
(defmethod event-type :system [_]
  (s/keys :req-un [::id ::name ::type ::level ::timestamp]
          :opt-un [::parent-id]))
(defmethod event-type :domain [_]
  (s/keys :req-un [::id ::name ::type ::level ::timestamp 
                   ::channel-id ::channel-name]
          :opt-un [::parent-id]))
(defmethod event-type :oms [_]
  (s/keys :req-un [::id ::name ::type ::level ::timestamp]))
(defmethod event-type :default [_]
  (s/keys :req-un [::type]))
(s/def ::event (s/multi-spec event-type :type))

(defn domain? [event]
  (and (s/valid? ::event event) (= :domain (:type event))))

(defn parsing-failed [parent-id message-type error-message]
  {:pre [(s/assert uuid? parent-id)
         (s/assert ::oms-message/type message-type)
         (s/assert ::error-message error-message)]
   :post [(s/assert ::event %)]}
  {:id (UUID/randomUUID)
   :timestamp (str (offset-date-time/ist-now))
   :name :system/parsing-failed
   :type :system
   :level :error
   :parent-id parent-id
   :payload {:type :system/parsing-failed
             :error-message error-message}})

(comment
  (s/check-asserts true)
  (parsing-failed (UUID/randomUUID) :ranging "expected!"))