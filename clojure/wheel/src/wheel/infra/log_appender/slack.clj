(ns wheel.infra.log-appender.slack
  (:require [wheel.slack.webhook :as slack]
            [wheel.infra.config :as config]
            [cheshire.core :as json]))

(defn- event->text [{event-name :name}]
  (str (namespace event-name) " " (name event-name)))

(defn- event->attachment [{:keys [id channel-id channel-name payload]}]
  {:color  :danger
   :fields [{:title "Channel Name"
             :value (or channel-name "N/A")
             :short true}
            {:title "Channel Id"
             :value (or channel-id "N/A")
             :short true}
            {:title "Event Id"
             :value id}
            {:title "Payload"
             :value (str
                     "```"
                     (json/generate-string (dissoc payload :type)
                                           {:pretty true})
                     "```")}]})

(defn- send-to-slack [{:keys [msg_]}]
  (let [event      (read-string (force msg_))
        text       (event->text event)
        attachment (event->attachment event)]
    (slack/post-message! (config/slack-log-webhook-url) text [attachment])))

(event->attachment {:name         :ranging/failed
                    :type         :domain
                    :level        :error
                    :channel-id   "UA"
                    :timestamp    "2019-10-04T15:56+05:30"
                    :id           #uuid "5040f252-5ff4-4e44-ab9c-72275a5a40ba"
                    :channel-name :tata-cliq})

(def appender {:enabled?  true
               :output-fn :inherit
               :async?    true
               :min-level :error
               :fn        send-to-slack})

(comment
  (event->text {:name :ranging/failed}))