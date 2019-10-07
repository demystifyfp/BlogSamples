(ns wheel.infra.log-appender.database
  (:require [wheel.model.event :as event]))

(defn- append-to-db [{:keys [msg_]}]
  (let [evnt (read-string (force msg_))]
    (event/create! evnt)))

(def appender {:enabled?  true
               :output-fn :inherit
               :async?    true
               :fn        append-to-db})
