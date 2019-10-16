(ns wheel.infra.log-appender.slack
  (:require [wheel.slack.webhook :as slack]
            [wheel.infra.config :as config]
            [cheshire.core :as json]))

(defn- event->text [{event-name :name}]
  (str (namespace event-name) " " (name event-name)))

(defn- event->attachment [{:keys [id channel-id channel-name parent-id payload]
                           :or   {channel-name "N/A"
                                  channel-id   "N/A"
                                  parent-id    "N/A"}}]
  {:color  :danger
   :fields [{:title "Channel Name"
             :value channel-name
             :short true}
            {:title "Channel Id"
             :value channel-id
             :short true}
            {:title "Event Id"
             :value id
             :short true}
            {:title "Parent Id"
             :value parent-id
             :short true}
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

(def appender
  {:enabled?  true
   :output-fn :inherit
   :async?    true
   :min-level :error
   :fn        send-to-slack})

(comment
  (event->text {:name :ranging/failed}))