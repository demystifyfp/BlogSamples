(ns wheel.middleware.event
  (:require [clojure.spec.alpha :as s]
            [wheel.marketplace.channel :as channel]
            [wheel.offset-date-time :as offset-date-time]
            [clojure.stacktrace :as stacktrace]))

(s/def ::id uuid?)
(s/def ::parent-id ::id)
(s/def ::name qualified-keyword?)
(s/def ::level #{:info :warn :debug :error :fatal})
(s/def ::timestamp ::offset-date-time/ist-timestamp)
(s/def ::type #{:domain :system :oms})

(s/def ::channel-id ::channel/id)
(s/def ::channel-name ::channel/name)

(s/def ::oms-event-name #{:oms/items-ranged})
(s/def ::domain-event-name #{:ranging/succeeded})
(s/def ::error-event-name #{:ranging/failed})

(s/def ::message (complement clojure.string/blank?))
(s/def ::stacktrace (complement clojure.string/blank?))

(defmulti payload-type :type)
(defmethod payload-type :oms/items-ranged [_]
  (s/keys :req-un [::message]))
(defmethod payload-type :ranging/failed [_]
  (s/keys :req-un [::message ::stacktrace]))
(defmethod payload-type :ranging/succeeded [_] map?)
(defmethod payload-type :default [_]
  (s/keys :req-un [::type]))
(s/def ::payload (s/multi-spec payload-type :type))

(defmulti event-type :type)
(defmethod event-type :system [_]
  (s/keys :req-un [::id ::name ::type ::level ::timestamp ::payload]
          :opt-un [::parent-id]))
(defmethod event-type :oms [_]
  (s/keys :req-un [::id ::name ::type ::level ::timestamp ::payload]))
(defmethod event-type :domain [_]
  (s/keys :req-un [::id ::name ::type ::level ::timestamp 
                   ::channel-id ::channel-name ::payload]
          :opt-un [::parent-id]))
(defmethod event-type :default [_]
  (s/keys :req-un [::type]))

(s/def ::event (s/multi-spec event-type :type))

(defn domain-or-oms? [event]
  (and (s/valid? ::event event) (#{:domain :oms} (:type event))))

(defn new-oms-event [event-name message]
  {:post [(s/assert ::event %)]}
  {:name      event-name
   :type      :oms
   :level     :info
   :timestamp (str (offset-date-time/ist-now))
   :id        (java.util.UUID/randomUUID)
   :payload   {:message message
               :type event-name}})


(defn- ex->map [ex]
  {:message     (with-out-str (stacktrace/print-throwable ex))
   :stacktrace (with-out-str (stacktrace/print-cause-trace ex))})

(defn new-error-event 
  ([event-name ex parent-id]
   (new-error-event event-name ex parent-id nil nil))
  ([event-name ex parent-id channel-id channel-name]
   {:post [(s/assert ::event %)]}
   (let [evt {:name         event-name
              :type         :domain
              :parent-id    parent-id
              :level        :error
              :timestamp    (str (offset-date-time/ist-now))
              :id           (java.util.UUID/randomUUID)
              :payload      (assoc (ex->map ex) :type event-name)}]
     (if (and channel-id channel-name)
       (assoc evt 
              :type :domain 
              :channel-id channel-id 
              :channel-name channel-name)
       (assoc evt :type :system)))))

(defn new-domain-event [event-name parent-id channel-id channel-name payload]
  {:post [(s/assert ::event %)]}
  {:name         event-name
   :level        :info
   :parent-id    parent-id
   :channel-id   channel-id
   :channel-name channel-name
   :type         :domain
   :timestamp    (str (offset-date-time/ist-now))
   :id           (java.util.UUID/randomUUID)
   :payload (assoc payload :type event-name)})

(comment
  (s/check-asserts false)
  (new-error-event :ranging/failed (Exception. "foo") (java.util.UUID/randomUUID))
  (ex-data (Exception. "foo"))
  (s/check-asserts true)
  (new-system-event :oms/items-ranged :info {:message ""})
  (s/valid?
   ::event
   {:name         :ranging/succeeded
    :type         :domain
    :channel-id   1
    :level        :info
    :timestamp    "2019-10-01T12:30+05:30"
    :id           (java.util.UUID/randomUUID)
    :channel-name :tata-cliq})
  (s/valid?
   ::event
   {:name      :db.migration/failed
    :type      :system
    :level     :fatal
    :timestamp "2019-10-01T12:30+05:30"
    :id        (java.util.UUID/randomUUID)}))