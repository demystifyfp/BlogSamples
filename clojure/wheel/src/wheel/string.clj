(ns wheel.string
  (:require [clojure.string :as str]))

(defn not-blank? [s]
  (and (string? s) (not (str/blank? s))))