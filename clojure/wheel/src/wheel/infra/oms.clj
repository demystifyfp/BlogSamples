(ns wheel.infra.oms
  (:require [wheel.infra.config :as config]
            [wheel.middleware.event :as event]
            [wheel.infra.ibmmq :as ibmmq]
            [mount.core :as mount]
            [wheel.infra.log :as log])
  (:import (javax.jms MessageListener Message)
           (javax.jms Session)))

(defn- message-listener [system-event-name error-event-name on-message-handler]
  (proxy [MessageListener] []
    (onMessage [^Message msg]
      (let [body         (.getBody msg String)
            parent-event (event/new-system-event system-event-name
                                                 :info {:message body})
            parent-id    (:id parent-event)]
        (try
          (let [events                            (on-message-handler {:message   body
                                                                       :parent-id parent-id})
                {:keys [channel-id channel-name]} (first events)]
            (-> (event/to-domain-event parent-event channel-id channel-name)
                (cons events)
                (doto prn)
                log/write-all!))
          (catch Throwable ex
            (if-let [{:keys [channel-id channel-name exception]} (ex-data ex)]
              (log/write-all!
               [(event/to-domain-event parent-event channel-id channel-name)
                (event/new-domain-error error-event-name exception
                                        parent-id channel-id channel-name)])
              (log/write-all!
               [parent-event (event/new-system-error error-event-name ex parent-id)])))
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

(defn- generic-msg-handler [{:keys [parent-id message]}]
  (case message
    "1" (throw (Exception. "Something went wrong"))
    "2" (throw (ex-info "Something went wrong in a channel"
                        {:channel-id   "UA"
                         :channel-name :tata-cliq
                         :exception    (Exception. "Something went wrong!")}))
    [(event/new-domain-event :ranging/succeeded :info parent-id "UA" :tata-cliq)]))

(mount/defstate ranging-consumer
  :start (let [queue-name (:queue (config/ranging))
               listener   (message-listener :oms/items-ranged :ranging/failed generic-msg-handler)]
           (start-consumer queue-name jms-ranging-session listener))
  :stop (stop ranging-consumer))