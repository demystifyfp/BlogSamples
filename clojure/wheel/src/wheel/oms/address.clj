(ns wheel.oms.address
  (:require [clojure.spec.alpha :as s]
            [wheel.string :as w-str]))

(s/def ::first-name w-str/not-blank?)
(s/def ::last-name w-str/not-blank?)
(s/def ::line1 w-str/not-blank?)
(s/def ::line2 w-str/not-blank?)
(s/def ::city w-str/not-blank?)
(s/def ::state w-str/not-blank?)
(s/def ::pincode (s/int-in 110001 855118))

(s/def ::address (s/keys :req-un [::first-name ::line1 ::city ::state ::pincode]
                          :opt-un [::last-name ::line2]))

(s/def ::shipping ::address)
(s/def ::billing ::address)