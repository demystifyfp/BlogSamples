(ns wheel.oms.order
  (:require [clojure.spec.alpha :as s]
            [wheel.oms.address :as addr]
            [wheel.oms.payment :as payment]
            [wheel.oms.order-line :as order-line]
            [wheel.string :as w-str]))

(s/def ::order-no w-str/not-blank?)
(s/def ::payments (s/coll-of ::payment/payment :min-count 1))
(s/def ::order-lines (s/coll-of ::order-line/order-line :min-count 1))
(s/def ::order (s/keys :req-un [::order-no ::addr/shipping ::addr/billing ::payments
                                ::order-lines]))

(comment
  (s/valid? ::order {:order-no "181219-001-345786"
                     :payments [{:amount 900M
                                 :reference-id "000000-1545216772601"}]
                     :order-lines [{:id "200374"
                                    :sale-price 900M}]
                     :billing  {:first-name "Tamizhvendan"
                                :last-name  "Sembiyan"
                                :line1      "Plot No 222"
                                :line2      "Ashok Nagar 42nd Street"
                                :city       "Chennai"
                                :state      "TamilNadu"
                                :pincode    600001}
                     :shipping {:first-name "Tamizhvendan"
                                :last-name  "Sembiyan"
                                :line1      "Plot No 222"
                                :line2      "Ashok Nagar 42nd Street"
                                :city       "Chennai"
                                :state      "TamilNadu"
                                :pincode    600001}}))