(ns wheel.infra.core
  (:require [mount.core :as mount]
            [wheel.infra.log :as log]
            [clojure.spec.alpha :as s]
            [wheel.infra.config :as config]
            [wheel.infra.database :as db]
            [wheel.infra.ibmmq :as ibmmq]
            [wheel.infra.oms :as oms]
            [mount-up.core :as mu]))

(defn- on-mount-error [ex _]
  (let [root (.getMessage (.getCause ex))]
    (log/fatal (str (.getMessage ex) " \"" root \"))))

(defn on-mount-upndown [{:keys [name action]}]
  (case action
    :up (prn ">> starting.." name)
    :down (prn "<< stopping.." name)))

(defn start-app
  ([]
   (start-app true))
  ([check-asserts]
   (log/init)
   (s/check-asserts check-asserts)
   (mu/on-upndown :guard (mu/try-catch on-mount-error) :wrap-in)
   (mu/on-upndown :info on-mount-upndown :before)
   (mount/start)))

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