(ns wheel.middleware.event
  (:require [clojure.spec.alpha :as s]
            [wheel.marketplace.channel :as channel]))

(s/def ::id uuid?)
(s/def ::parent-id ::id)
(s/def ::name qualified-keyword?)
(s/def ::level #{:info :warn :debug :error :fatal})
(s/def ::type #{:domain :system})

(s/def ::channel-id integer?)
(s/def ::channel-name ::channel/name)

(defmulti event-type :type)
(defmethod event-type :domain [_]
  (s/keys :req-un [::id ::name ::type ::channel-id ::channel-name ::level]
          :opt-un [::parent-id]))
(defmethod event-type :system [_]
  (s/keys :req-un [::id ::name ::type ::level]
          :opt-un [::parent-id]))
(defmethod event-type :default [_]
  (s/keys :req-un [::type]))
(s/def ::event (s/multi-spec event-type :type))

(comment
  (s/valid?
   ::event
   {:name :ranging/succeeded
    :type :domain
    :channel-id 1
    :level :info
    :id #uuid "8e70f4df-fc3d-4474-8ee9-33f4c87bf934"
    :channel-name :tata-cliq})
  (s/valid?
   ::event
   {:name :db.migration/failed
    :type :system
    :level :fatal
    :id #uuid "8e70f4df-fc3d-4474-8ee9-33f4c87bf934"}))