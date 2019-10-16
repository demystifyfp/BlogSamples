(ns wheel.infra.log
  (:require [taoensso.timbre :as timbre]
            [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [wheel.middleware.event :as event]
            [wheel.infra.log-appender.database :as database]
            [wheel.infra.log-appender.slack :as slack]))

(defn- json-output [{:keys [msg_]}]
  (let [event (read-string (force msg_))]
    (json/generate-string event)))

(defn init []
  (timbre/merge-config! {:output-fn json-output
                         :appenders {:database database/appender
                                     :slack    slack/appender}}))

(defn write! [{:keys [level]
               :as   event}]
  {:pre [(s/assert ::event/event event)]}
  (case level
    :info (timbre/info event)
    :debug (timbre/debug event)
    :warn (timbre/warn event)
    :error (timbre/error event)
    :fatal (timbre/fatal event)))

(defn write-all! [events]
  (prn "~~>" events)
  (run! write! events))

(defn fatal [ex]
  (timbre/fatal ex))

(comment
  (timbre/info "Hello Timbre!")
  (timbre/info {:Hello "Timbre!"})
  (init)
  (timbre/info {:name :ranging/succeeded})
  (s/check-asserts true)
  (write! {:level :info
           :name  :foo})
  (s/check-asserts false)
  (s/assert ::event/event {:level :info
                           :name  :foo})
  (write! {:name         :ranging/failed
           :type         :domain
           :level        :error
           :channel-id   "UA"
           :timestamp    "2019-10-04T15:56+05:30"
           :id           (java.util.UUID/randomUUID)
           :channel-name :tata-cliq}))
