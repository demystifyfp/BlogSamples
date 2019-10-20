(ns wheel.oms.payment
  (:require [clojure.spec.alpha :as s]
            [wheel.string :as w-str]))

(s/def ::amount (s/and decimal? pos?))
(s/def ::reference-id w-str/not-blank?)

(s/def ::payment (s/keys :req-un [::amount ::reference-id]))

