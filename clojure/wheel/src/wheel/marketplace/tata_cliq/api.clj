(ns wheel.marketplace.tata-cliq.api
  (:require [clj-http.client :as http]
            [wheel.marketplace.tata-cliq.order :as tata-cliq-order]))

(defn ranging [{:keys [base-url bearer-token]} channel-id items]
  (let [url         (str base-url "/channels/" channel-id "/ranging")
        auth-header (str "Bearer " bearer-token)]
    (http/post url {:form-params  items
                    :content-type :json
                    :headers      {:authorization auth-header}})))

(defn new-orders [{:keys [base-url bearer-token]} channel-id]
  (let [url         (str base-url "/channels/" channel-id "/new-orders")
        auth-header (str "Bearer " bearer-token)]
    (-> (http/get url {:headers {:authorization auth-header}})
        :body
        tata-cliq-order/parse-new-orders)))

(comment
  (BigDecimal. "1")
  (Integer/parseInt "600001")
  (new-orders {:base-url     "http://localhost:3000"
               :bearer-token "top-secret!"}
              "UB"))