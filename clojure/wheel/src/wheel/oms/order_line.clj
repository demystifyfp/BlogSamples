(ns wheel.oms.order-line
  (:require [wheel.oms.item :as item]
            [clojure.spec.alpha :as s]))

(s/def ::sale-price (s/and decimal? pos?))

(s/def ::order-line (s/keys :req-un [::item/id ::sale-price]))