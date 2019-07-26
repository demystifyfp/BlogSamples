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

(comment
  (mount/start)
  (database)
  (mount/stop))