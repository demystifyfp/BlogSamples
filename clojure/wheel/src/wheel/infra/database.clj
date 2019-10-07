(ns wheel.infra.database
  (:require [wheel.infra.config :as config]
            [mount.core :as mount]
            [hikari-cp.core :as hikari]
            [toucan.db :as db]
            [cheshire.core :as json]
            [toucan.models :as models])
  (:import [org.flywaydb.core Flyway]
           [org.postgresql.util PGobject]))

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

(defn- pg-object-fn [pg-enum-type]
  (fn [value]
    (doto (PGobject.)
      (.setType pg-enum-type)
      (.setValue (name value)))))

(defn- to-pg-jsonb [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (json/generate-string value))))

(defn- configure-toucan []
  (db/set-default-db-connection! {:datasource datasource})
  (models/set-root-namespace! 'wheel.model)
  (db/set-default-automatically-convert-dashes-and-underscores! true)
  (models/add-type! :event-level
                    :in (pg-object-fn "event_level")
                    :out keyword)
  (models/add-type! :event-type
                    :in (pg-object-fn "event_type")
                    :out keyword)
  (models/add-type! :jsonb
                    :in to-pg-jsonb
                    :out #(json/parse-string (.getValue %) true))
  (models/add-type! :channel-name
                    :in (pg-object-fn "channel_name")
                    :out keyword))

(mount/defstate toucan
  :start (configure-toucan))

(comment
  (user/reset)
  (migrate)
  (mount/stop))