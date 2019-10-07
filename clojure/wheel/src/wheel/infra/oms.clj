(ns wheel.infra.oms
  (:require [wheel.infra.config :as config]
            [wheel.middleware.event :as event]
            [wheel.infra.ibmmq :as ibmmq]
            [mount.core :as mount]
            [wheel.infra.log :as log])
  (:import [javax.jms MessageListener Message]
           [javax.jms Session]))

(defn- on-handler-exception [ex error-event-name oms-event]
  (if-let [{:keys [channel-id channel-name exception]} (ex-data ex)]
    (log/write-all!
     [oms-event
      (event/new-error-event error-event-name exception (:id oms-event)
                             channel-id channel-name)])
    (log/write-all!
     [oms-event
      (event/new-error-event error-event-name ex (:id oms-event))])))

(defn- message-listener [oms-event-name error-event-name on-message-handler]
  (proxy [MessageListener] []
    (onMessage [^Message msg]
      (let [message   (.getBody msg String)
            oms-event (event/new-oms-event oms-event-name message)]
        (try
          (->> (on-message-handler (:id oms-event) message)
               (cons oms-event)
               log/write-all!)
          (catch Throwable ex
            (try
              (on-handler-exception ex error-event-name oms-event)
              (catch Throwable e
                (log/fatal e))))
          (finally (.acknowledge msg)))))))

(defn start-consumer [queue-name jms-session listener]
  (let [ibmmq-queue-name (str "queue:///" queue-name)
        destination      (.createQueue jms-session ibmmq-queue-name)
        consumer         (.createConsumer jms-session destination)]
    (.setMessageListener consumer listener)
    consumer))

(defn stop [stoppable]
  (.close stoppable))

(mount/defstate jms-ranging-session
  :start (.createSession ibmmq/jms-conn false Session/CLIENT_ACKNOWLEDGE)
  :stop (stop jms-ranging-session))

(defn- generic-msg-handler [parent-id message]
  (case message
    "1" (throw (Exception. "Something went wrong"))
    "2" (throw (ex-info "Something went wrong in a channel"
                        {:channel-id   "UA"
                         :channel-name :tata-cliq
                         :exception    (Exception. "Something went wrong!")}))
    [(event/new-domain-event :ranging/succeeded parent-id "UA" :tata-cliq {})]))

(mount/defstate ranging-consumer
  :start (let [queue-name (:queue (config/ranging))
               listener   (message-listener :oms/items-ranged :ranging/failed generic-msg-handler)]
           (start-consumer queue-name jms-ranging-session listener))
  :stop (stop ranging-consumer))