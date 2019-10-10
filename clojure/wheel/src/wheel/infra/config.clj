(ns wheel.infra.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [mount.core :as mount]))

(defn- read-config []
  (aero/read-config (io/resource "config.edn")))

(mount/defstate root
  :start (read-config))

(defn database []
  (get-in root [:app :database]))

(defn slack-log-webhook-url []
  (get-in root [:app :log :slack :webhook-url]))

(defn mq []
  (get-in root [:app :mq]))

(defn oms-settings []
  (get-in root [:settings :oms]))

(comment
  (mount/start)
  (database)
  (mount/stop))