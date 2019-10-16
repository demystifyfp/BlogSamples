(ns wheel.middleware.event
  (:require [clojure.spec.alpha :as s]
            [clojure.stacktrace :as stacktrace]
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
                             :system/channel-not-found
                             :system/processing-failed})

(s/def ::name (s/or :oms ::oms-event-name 
                    :domain ::domain-event-name
                    :system ::system-event-name))

(s/def ::level #{:info :warn :debug :error :fatal})
(s/def ::timestamp ::offset-date-time/ist-timestamp)
(s/def ::type #{:domain :system :oms})

(s/def ::channel-id ::channel/id)
(s/def ::channel-name ::channel/name)


(defmulti payload-type :type)

(defmethod payload-type :oms/items-ranged [_]
  (s/keys :req-un [::oms-message/message]))

(s/def ::item-ids (s/coll-of ::item/id :min-count 1))
(defmethod payload-type :ranging/succeeded [_] 
  (s/keys :req-un [::item-ids]))

(s/def ::error-message (s/and string? (complement clojure.string/blank?)))
(s/def ::stacktrace (s/and string? (complement clojure.string/blank?)))
(defmethod payload-type :ranging/failed [_]
  (s/keys :req-un [::error-message ::stacktrace]))

(s/def ::message-type ::oms-message/type)
(defmethod payload-type :system/parsing-failed [_]
  (s/keys :req-un [::error-message ::message-type]))

(defmethod payload-type :system/channel-not-found [_]
  (s/keys :req-un [::channel-id]))

(defmethod payload-type :system/processing-failed [_]
  (s/keys :req-un [::error-message ::stacktrace]))

(defmethod payload-type :default [_]
  (s/keys :req-un [::type]))
(s/def ::payload (s/multi-spec payload-type :type))

(defmulti event-type :type)
(defmethod event-type :system [_]
  (s/keys :req-un [::id ::name ::type ::level ::timestamp ::payload]
          :opt-un [::parent-id]))
(defmethod event-type :domain [_]
  (s/keys :req-un [::id ::name ::type ::level ::timestamp 
                   ::channel-id ::channel-name ::payload]
          :opt-un [::parent-id]))
(defmethod event-type :oms [_]
  (s/keys :req-un [::id ::name ::type ::level ::timestamp ::payload]))
(defmethod event-type :default [_]
  (s/keys :req-un [::type]))
(s/def ::event (s/multi-spec event-type :type))

(defn domain? [event]
  (and (s/valid? ::event event) (= :domain (:type event))))

(defn- event [name payload &{:keys [level type parent-id]
                             :or   {level :info
                                    type  :domain}}]
  {:post [(s/assert ::event %)]}
  (let [event {:id        (UUID/randomUUID)
               :timestamp (str (offset-date-time/ist-now))
               :name      name
               :level     level
               :type      type
               :payload   (assoc payload :type name)}]
    (if parent-id
      (assoc event :parent-id parent-id)
      event)))

(defn oms [oms-event-name message]
  {:pre [(s/assert ::oms-event-name oms-event-name)
         (s/assert ::oms-message/message message)]
   :post [(s/assert ::event %)]}
  (event oms-event-name 
         {:message message}
         :type :oms))

(defn- ex->map [ex]
  {:error-message    (with-out-str (stacktrace/print-throwable ex))
   :stacktrace (with-out-str (stacktrace/print-stack-trace ex 3))})

(defn processing-failed [ex]
  {:post [(s/assert ::event %)]}
  (event :system/processing-failed
         (ex->map ex)
         :type :system
         :level :error))

(defn parsing-failed [parent-id message-type error-message]
  {:pre [(s/assert uuid? parent-id)
         (s/assert ::oms-message/type message-type)
         (s/assert ::error-message error-message)]
   :post [(s/assert ::event %)]}
  (event :system/parsing-failed 
         {:error-message error-message
          :message-type message-type}
         :parent-id parent-id
         :type :system
         :level :error))

(comment
  (s/check-asserts true)
  (oms :oms/items-ranged "hello")
  (processing-failed (Throwable. "foo"))
  (parsing-failed (UUID/randomUUID) :ranging "expected!"))