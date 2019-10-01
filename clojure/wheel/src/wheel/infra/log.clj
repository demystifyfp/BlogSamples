(ns wheel.infra.log
  (:require [taoensso.timbre :as timbre]
            [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [wheel.middleware.event :as event]))

(defn- json-output [{:keys [msg_]}]
  (let [event (read-string (force msg_))]
    (json/generate-string event)))

(defn init []
  (timbre/merge-config! {:output-fn json-output}))

(defn write! [{:keys [level]
               :as event}]
  {:pre [(s/assert ::event/event event)]}
  (case level
    :info (timbre/info event)
    :debug (timbre/debug event)
    :warn (timbre/warn event)
    :error (timbre/error event)
    :fatal (timbre/fatal event)))

(defn write-all! [events]
  (run! write! events))

(comment
  (timbre/info "Hello Timbre!")
  (timbre/info {:Hello "Timbre!"})
  (init)
  (timbre/info {:name :ranging/succeeded})
  (s/check-asserts true)
  (write! {:level :info :name :foo})
  (s/check-asserts false)
  (s/assert ::event/event {:level :info :name :foo})
  (write! {:name :ranging/succeeded
           :type :domain
           :level :info
           :channel-id 1
           :timestamp "2019-10-01T21:56+05:30"
           :id #uuid "8e70f4df-fc3d-4474-8ee9-33f4c87bf934"
           :parent-id #uuid "20c84be2-7ec2-4127-8d39-a9cb4065a9ce"
           :channel-name :tata-cliq})
  )
