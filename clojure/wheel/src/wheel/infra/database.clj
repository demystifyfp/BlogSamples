(ns wheel.infra.database
  (:require [wheel.infra.config :as config]
            [mount.core :as mount]
            [hikari-cp.core :as hikari])
  (:import [org.flywaydb.core Flyway]))

(defn- make-datasource []
  (hikari/make-datasource (config/database)))

(mount/defstate datasource
  :start (make-datasource)
  :stop (hikari/close-datasource datasource))

(defn migrate []
  (.. (Flyway/configure)
      (dataSource datasource)
      (locations (into-array String ["classpath:db/migration"]))
      load
      migrate))

(comment
  (mount/start)
  (migrate)
  (mount/stop))