(ns wheel.infra.core
  (:require [mount.core :as mount]
            [wheel.infra.config :as config]
            [wheel.infra.database :as db]))

(defn start-app []
  (mount/start))

(defn stop-app []
  (mount/stop))

(defn migrate-database []
  (mount/start #'config/root #'db/datasource)
  (db/migrate)
  (mount/stop))

(comment
  (start-app)
  (stop-app)
  (migrate-database))