(ns restful-crud.user
  (:require [schema.core :as s]
            [restful-crud.models.user :refer [User]]
            [buddy.hashers :as hashers]
            [clojure.set :refer [rename-keys]]
            [restful-crud.restful :as restful]
            [restful-crud.string-util :as str]))

(defn valid-username? [name]
  (str/non-blank-with-max-length? 50 name))

(defn valid-password? [password]
  (str/length-in-range? 5 50 password))

(s/defschema UserRequestSchema
  {:username (s/constrained s/Str valid-username?)
   :password (s/constrained s/Str valid-password?)
   :email (s/constrained s/Str str/email?)})

(defn canonicalize-user-req [user-req]
  (-> (update user-req :password hashers/derive)
      (rename-keys {:password :password_hash})))

(def user-entity-route
  (restful/resource {:model User
                     :name "users"
                     :req-schema UserRequestSchema
                     :leave #(dissoc % :password_hash)
                     :enter canonicalize-user-req}))