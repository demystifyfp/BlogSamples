(ns wheel.infra.oms
  (:require [wheel.infra.ibmmq :as ibmmq]
            [mount.core :as mount]
            [wheel.infra.config :as config]
            [wheel.middleware.event :as event]
            [wheel.middleware.core :as middleware]
            [wheel.infra.log :as log])
  (:import [javax.jms MessageListener Message]
           [javax.jms Session]))

(defn stop [stoppable]
  (.close stoppable))

(mount/defstate jms-ranging-session
  :start (.createSession ibmmq/jms-conn false Session/AUTO_ACKNOWLEDGE)
  :stop (stop jms-ranging-session))

(defn- message-listener [message-type oms-event-name]
  (proxy [MessageListener] []
    (onMessage [^Message msg]
      (let [message                     (.getBody msg String)
            {:keys [id]
             :as   oms-event} (event/oms oms-event-name message)]
        (try
          (->> (middleware/handle {:id      id
                                   :message message
                                   :type    message-type})
               (cons oms-event)
               log/write-all!)
          (catch Throwable ex
            (prn ex)))))))

(defn- start-consumer [queue-name jms-session listener]
  (let [ibmmq-queue-name (str "queue:///" queue-name)
        destination      (.createQueue jms-session ibmmq-queue-name)
        consumer         (.createConsumer jms-session destination)]
    (.setMessageListener consumer listener)
    consumer))

(mount/defstate ranging-consumer
  :start (let [queue-name (:ranging-queue-name (config/oms-settings))
               listener   (message-listener :ranging :oms/items-ranged)]
           (start-consumer queue-name jms-ranging-session listener))
  :stop (stop ranging-consumer))