(ns restful-crud.restful
  (:require [compojure.api.sweet :refer [GET POST PUT DELETE routes]]
            [toucan.db :as db]
            [schema.core :as s]
            [ring.util.http-response :refer [ok not-found created]]))

(defn resource-id-path [name]
  (str "/" name "/:id"))

(defn id->created [name id]
  (created (str "/" name "/" id) {:id id}))

(defn create-route [{:keys [name model req-schema enter]}]
  (let [enter-interceptor (or enter identity)
        path (str "/" name)]
    (POST path http-req
      :body [req-body req-schema]
      (->> (enter-interceptor req-body)
           (db/insert! model)
           :id
           (id->created name)))))

(defn entity->response [entity]
  (if entity
    (ok entity)
    (not-found)))

(defn get-by-id-route [{:keys [name model leave]}]
  (let [leave-interceptor (or leave identity)
        path (resource-id-path name)]
    (GET path []
      :path-params [id :- s/Int]
      (-> (model id)
          leave-interceptor
          entity->response))))

(defn get-all-route [{:keys [name model leave]}]
  (let [leave-interceptor (or leave identity)
        path (str "/" name)]
    (GET path []
      (->> (db/select model)
           (map leave-interceptor)
           ok))))

(defn update-route [{:keys [name model req-schema enter]}]
  (let [enter-interceptor (or enter identity)
        path (resource-id-path name)]
    (PUT path http-req
      :path-params [id :- s/Int]
      :body [req-body req-schema]
      (db/update! model id (enter-interceptor req-body))
      (ok))))

(defn delete-route [{:keys [name model]}]
  (let [path (resource-id-path name)]
    (DELETE path []
      :path-params [id :- s/Int]
      (db/delete! model :id id)
      (ok))))

(defn resource [resource-config]
  (routes
   (create-route resource-config)
   (get-by-id-route resource-config)
   (get-all-route resource-config)
   (update-route resource-config)
   (delete-route resource-config)))