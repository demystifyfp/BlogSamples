(ns resultful-crud.restful
  (:require [compojure.api.sweet :refer [routes]]))

(defn create-route [{:keys [name model req-schema]}]
  42)

(defn resource [resource-config]
  (routes
   (create-route resource-config)))