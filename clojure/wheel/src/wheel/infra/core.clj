(ns wheel.infra.core
  (:require [mount.core :as mount]
            [wheel.infra.log :as log]
            [clojure.spec.alpha :as s]
            [wheel.infra.config :as config]
            [wheel.infra.database :as db]
            [wheel.infra.cron.core :as cron]
            [wheel.infra.ibmmq :as ibmmq]
            [wheel.infra.oms :as oms]
            [wheel.middleware.ranging :as ranging]
            [wheel.marketplace.tata-cliq.core :as tata-cliq]))

(defn start-app
  ([]
   (start-app true))
  ([check-asserts]
   (log/init)
   (s/check-asserts check-asserts)
   (mount/start)
   (cron/init)))

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