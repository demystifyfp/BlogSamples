(ns user
  (:require [wheel.infra.core :refer [start-app stop-app migrate-database] :as infra]
            [clojure.tools.namespace.repl :as repl]))

(defn reset []
  (stop-app)
  (repl/refresh :after 'infra/start-app))