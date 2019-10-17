(ns wheel.marketplace.tata-cliq.api
  (:require [clj-http.client :as http]))

(defn ranging [{:keys [base-url bearer-token]} channel-id items]
  (let [url         (str base-url "/channels/" channel-id "/ranging")
        auth-header (str "Bearer " bearer-token)]
    (http/post url {:form-params  items
                    :content-type :json
                    :headers      {:authorization auth-header}})))

(comment
  (ranging {:base-url     "http://localhost:3000"
            :bearer-token "top-secret!"}
           "UB" [{:sku "SKU1"
                  :ean "EAN1"}]))