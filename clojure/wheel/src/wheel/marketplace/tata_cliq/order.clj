(ns wheel.marketplace.tata-cliq.order
  (:require [wheel.oms.order :as order]
            [clojure.spec.alpha :as s]
            [clojure.set :as set]
            [wheel.xml :as w-xml]))

(defn- vectorize [x]
  (if (vector? x)
    x
    (vector x)))

(defn- coarce-new-order-lines [new-order-lines]
  (map (fn [ol]
         (update ol :price #(BigDecimal. %))) new-order-lines))

(defn- coarce-new-orders [orders]
  (map (fn [order]
         (-> (update-in order [:address-info :billing :pincode] #(Integer/parseInt %))
             (update-in [:address-info :shipping :pincode] #(Integer/parseInt %))
             (update-in [:payment-info :payment-cost] #(BigDecimal. %))
             (assoc :order-lines (coarce-new-order-lines (:order-lines order)))))
       orders))

(defn- cleanse-new-order [orders]
  (map #(assoc % :order-lines
               (vectorize (get-in % [:order-lines :order-line]))) orders))

(defn parse-new-orders [xml-response]
  (-> (w-xml/xml-str->map xml-response)
      (get-in [:orders :order])
      vectorize
      cleanse-new-order
      coarce-new-orders))

(defn- to-oms-address [tata-cliq-address]
  (set/rename-keys tata-cliq-address {:address1 :line1
                                      :address2 :line2}))

(defn to-oms-order [tata-cliq-order]
  {:post [(s/assert ::order/order %)]}
  (let [{:keys [address-info order-no order-lines payment-info]} tata-cliq-order
        {:keys [shipping billing]}                               address-info
        {:keys [payment-cost payment-id]}                        payment-info]
    {:billing-address  (to-oms-address billing)
     :shipping-address (to-oms-address shipping)
     :order-no         order-no
     :payments         [{:amount       payment-cost
                         :reference-id payment-id}]
     :order-lines      (map (fn [{:keys [article-number price]}]
                              {:id         article-number
                               :sale-price price})
                            order-lines)}))

(comment
  (s/check-asserts true)
  (map to-oms-order [{:address-info {:billing  {:address1   "Plot No 222"
                                                :address2   "Ashok Nagar 42nd Street"
                                                :city       "Chennai"
                                                :first-name "Tamizhvendan"
                                                :last-name  "Sembiyan"
                                                :pincode    600001
                                                :state      "TamilNadu"}
                                     :shipping {:address1   "Plot No 222"
                                                :address2   "Ashok Nagar 42nd Street"
                                                :city       "Chennai"
                                                :first-name "Tamizhvendan"
                                                :last-name  "Sembiyan"
                                                :pincode    600001
                                                :state      "TamilNadu"}}
                      :order-lines  [{:article-number "200374"
                                      :price          900.0M
                                      :transaction-id "200058001702351"}]
                      :order-no     "181219-001-345786"
                      :payment-info {:payment-cost 900.0M
                                     :payment-id   "000000-1545216772601"}}]))