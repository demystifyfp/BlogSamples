(ns wheel.marketplace.channel
  (:require [clojure.spec.alpha :as s]))

(s/def ::name #{:tata-cliq :amazon :flipkart})