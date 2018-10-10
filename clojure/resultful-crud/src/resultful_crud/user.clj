(ns resultful-crud.user
  (:require [schema.core :as s]
            [resultful-crud.models.user :refer [User]]
            [buddy.hashers :as hashers]
            [clojure.set :refer [rename-keys]]
            [toucan.db :as db]
            [ring.util.http-response :refer [ok created]]
            [compojure.api.sweet :refer [GET POST PUT DELETE]]
            [resultful-crud.string-util :as str]))

(defn valid-username? [name]
  (str/non-blank-with-max-length? 50 name))

(defn valid-password? [password]
  (str/length-in-range? 5 50 password))

(s/defschema UserRequestSchema
  {:username (s/constrained s/Str valid-username?)
   :password (s/constrained s/Str valid-password?)
   :email (s/constrained s/Str str/email?)})

(defn id->created [id]
  (created (str "/users/" id) {:id id}))

(defn canocialize-user-req [user-req]
  (-> (update user-req :password hashers/derive)
      (rename-keys {:password :password_hash})))

(defn create-user-handler [create-user-req]
  (->> (canocialize-user-req create-user-req)
       (db/insert! User)
       :id
       id->created))

(defn delete-user-handler [user-id]
  (db/delete! User :id user-id)
  (ok))

(defn get-users-handler []
  (->> (db/select User)
       (map #(dissoc % :password_hash))
       ok))

(defn update-user-handler [id update-user-req]
  (db/update! User id (canocialize-user-req update-user-req))
  (ok))

(def user-routes
  [(POST "/users" []
     :body [create-user-req UserRequestSchema]
     (create-user-handler create-user-req))
   (GET "/users" []
     (get-users-handler))
   (PUT "/users/:id" []
     :path-params [id :- s/Int]
     :body [update-user-req UserRequestSchema]
     (update-user-handler id update-user-req))
   (DELETE "/users/:id" []
     :path-params [id :- s/Int]
     (delete-user-handler id))])