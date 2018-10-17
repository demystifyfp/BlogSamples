(ns restful-crud.book
  (:require [schema.core :as s]
            [restful-crud.models.book :refer [Book]]
            [restful-crud.restful :as restful]
            [restful-crud.string-util :as str]))

(defn valid-book-title? [title]
  (str/non-blank-with-max-length? 100 title))

(defn valid-year-published? [year]
  (<= 2000 year 2018))

(s/defschema BookRequestSchema
  {:title (s/constrained s/Str valid-book-title?)
   :year_published (s/constrained s/Int valid-year-published?)})

(def book-entity-route
  (restful/resource {:model Book
                     :name "books"
                     :req-schema BookRequestSchema}))