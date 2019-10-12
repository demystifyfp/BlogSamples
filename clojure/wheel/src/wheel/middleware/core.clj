(ns wheel.middleware.core
  (:require [clojure.spec.alpha :as s]
            [wheel.marketplace.channel :as channel]
            [wheel.infra.config :as config]))

(s/def ::message-type #{:ranging})

(defmulti validate-message (fn [msg-type msg]
                             msg-type))

(defmulti validate-spec (fn [msg-type msg]
                          msg-type))

(defmulti parse (fn [msg-type msg]
                  msg-type))

(defn handle [message-type message]
  (when-let [err (validate-message message-type message)]
    (throw (ex-info "invalid message" {:reason :validation-failed
                                       :error  err})))
  (when-let [err (validate-spec message-type message)]
    (throw (ex-info "invalid message" {:reason :validation-failed
                                       :error  err})))
  (for [{:keys [channel-id]
         :as   channel-msg} (parse message-type message)]
    (if-let [{:keys [channel-name]
              :as   channel-config} (config/channel channel-id)]
      (try
        (channel/handle channel-config channel-msg)
        (catch Throwable ex
          "todo"))
      ["todo"])))