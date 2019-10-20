(ns wheel.oms.order
  (:require [clojure.spec.alpha :as s]
            [wheel.oms.address :as addr]
            [wheel.oms.payment :as payment]
            [wheel.oms.order-line :as order-line]
            [wheel.string :as w-str]
            [clojure.data.xml :as xml]))

(s/def ::order-no w-str/not-blank?)
(s/def ::payments (s/coll-of ::payment/payment :min-count 1))
(s/def ::order-lines (s/coll-of ::order-line/order-line :min-count 1))
(s/def ::billing-address ::addr/address)
(s/def ::shipping-address ::addr/address)
(s/def ::order (s/keys :req-un [::order-no ::shipping-address ::billing-address ::payments
                                ::order-lines]))

(defn address-to-xml [{:keys [first-name last-name line1
                              line2 city state pincode]}]
  {:attrs {:FirstName first-name
           :LastName last-name
           :State state
           :City city
           :Pincode pincode}
   :ext [:Extn {:IRLAddressLine1 line1
                :IRLAddressLine2 line2}]})

(defn to-xml [order]
  {:pre [(s/assert ::order order)]}
  (let [{:keys [order-no billing-address order-lines 
                shipping-address payments]} order
        {bill-to-attrs :attrs
         bill-to-ext :ext} (address-to-xml billing-address)
        {ship-to-attrs :attrs
         ship-to-ext :ext} (address-to-xml shipping-address)]
    (-> [:Order {:OrderNo order-no}
         [:PersonInfoBillTo bill-to-attrs bill-to-ext]
         [:PersonInfoShipTo ship-to-attrs ship-to-ext]
         [:PaymentDetailsList 
          (map (fn [{:keys [amount reference-id]}]
                 [:PaymentDetails {:ProcessedAmount amount
                                   :Reference1 reference-id}]) payments)]
         [:OrderLines
          (map (fn [{:keys [id sale-price]}]
                 [:OrderLine 
                  [:Item {:ItemID id}]
                  [:LinePriceInfo {:LineTotal sale-price}]])
               order-lines)]]
        xml/sexp-as-element
        xml/indent-str)))

(comment
  (s/check-asserts true)
  (spit "test.xml" (to-xml {:order-no "181219-001-345786"
                :payments [{:amount 900M
                            :reference-id "000000-1545216772601"}]
                :order-lines [{:id "200374"
                               :sale-price 900M}]
                :billing-address  {:first-name "Tamizhvendan"
                                   :last-name  "Sembiyan"
                                   :line1      "Plot No 222"
                                   :line2      "Ashok Nagar 42nd Street"
                                   :city       "Chennai"
                                   :state      "TamilNadu"
                                   :pincode    600001}
                :shipping-address {:first-name "Tamizhvendan"
                                   :last-name  "Sembiyan"
                                   :line1      "Plot No 222"
                                   :line2      "Ashok Nagar 42nd Street"
                                   :city       "Chennai"
                                   :state      "TamilNadu"
                                   :pincode    600001}})))