(ns wheel.infra.oms
  (:require [wheel.infra.ibmmq :as ibmmq]
            [mount.core :as mount]
            [wheel.infra.config :as config])
  (:import [javax.jms MessageListener Message]
           [javax.jms Session]))

(defn stop [stoppable]
  (.close stoppable))

(mount/defstate jms-ranging-session
  :start (.createSession ibmmq/jms-conn false Session/AUTO_ACKNOWLEDGE)
  :stop (stop jms-ranging-session))

(defn- message-listener []
  (proxy [MessageListener] []
    (onMessage [^Message msg]
      (let [msg (.getBody msg String)]
        (prn "Received: " msg)))))

(defn- start-consumer [queue-name jms-session listener]
  (let [ibmmq-queue-name (str "queue:///" queue-name)
        destination      (.createQueue jms-session ibmmq-queue-name)
        consumer         (.createConsumer jms-session destination)]
    (.setMessageListener consumer listener)
    consumer))

(mount/defstate ranging-consumer
  :start (let [queue-name (:ranging-queue-name (config/oms-settings))
               listener   (message-listener)]
           (start-consumer queue-name jms-ranging-session listener))
  :stop (stop ranging-consumer))