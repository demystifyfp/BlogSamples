(ns wheel.core
  (:require [wheel.infra.core :as infra])
  (:gen-class))

(defn add-shutdown-hook []
  (.addShutdownHook (Runtime/getRuntime) (Thread. infra/stop-app)))

(defn -main
  [& args]
  (infra/start-app)
  (add-shutdown-hook)
  (println "Hello, World!"))
