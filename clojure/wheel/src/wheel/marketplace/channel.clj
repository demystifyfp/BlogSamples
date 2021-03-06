(ns wheel.marketplace.channel
  (:require [clojure.spec.alpha :as s]))

(s/def ::id (complement clojure.string/blank?))
(s/def ::name #{:tata-cliq :amazon :flipkart})

(defmulti allocate-order (fn [channel-id channel-config]
                           (:channel-name channel-config)))