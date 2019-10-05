(ns wheel.slack.webhook
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(defn post-message! [webhook-url text attachments]
  (let [body (json/generate-string {:text text
                                    :attachments attachments})]
    (http/post webhook-url {:content-type :json
                            :body body})))


(comment
  (post-message! webhook-url "ranging failed"
                 [{:color :danger
                   :fields [{:title "Channel Name"
                             :value :tata-cliq
                             :short true}
                            {:title "Channel Id"
                             :value "UA"
                             :short true}
                            {:title "Event Id"
                             :value "2f763cf7-d5d7-492c-a72d-4546bb547696"}]}]))