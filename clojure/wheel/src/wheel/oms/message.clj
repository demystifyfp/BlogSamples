(ns wheel.oms.message
  (:require [clojure.spec.alpha :as s]))

(s/def ::type #{:ranging})
(s/def ::id uuid?)
(s/def ::message (s/and string? (complement clojure.string/blank?)))

(s/def ::oms-message
       (s/keys :req-un [::type ::id ::messsage]))