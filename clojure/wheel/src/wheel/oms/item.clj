(ns wheel.oms.item
  (:require [clojure.spec.alpha :as s]))

(s/def ::id (s/and string? (complement clojure.string/blank?)))
(s/def ::ean (s/and string? (complement clojure.string/blank?)))

