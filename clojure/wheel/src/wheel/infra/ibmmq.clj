(ns wheel.infra.ibmmq
  (:import [com.ibm.msg.client.jms JmsFactoryFactory]
           [com.ibm.msg.client.wmq WMQConstants]
           [javax.jms ExceptionListener
            JMSException])
  (:require [wheel.infra.config :as config]
            [wheel.infra.log :as log]
            [mount.core :as mount]))

(defn- new-jms-conn [{:keys [host port channel qmgr user-id password]}]
  (let [ff (JmsFactoryFactory/getInstance WMQConstants/WMQ_PROVIDER)
        cf (.createConnectionFactory ff)]
    (doto cf
      (.setStringProperty WMQConstants/WMQ_HOST_NAME host)
      (.setIntProperty WMQConstants/WMQ_PORT port)
      (.setStringProperty WMQConstants/WMQ_CHANNEL channel)
      (.setIntProperty WMQConstants/WMQ_CONNECTION_MODE WMQConstants/WMQ_CM_CLIENT)
      (.setStringProperty WMQConstants/WMQ_QUEUE_MANAGER qmgr)
      (.setStringProperty WMQConstants/WMQ_APPLICATIONNAME "WHEEL")
      (.setBooleanProperty WMQConstants/USER_AUTHENTICATION_MQCSP true)
      (.setStringProperty WMQConstants/USERID user-id)
      (.setStringProperty WMQConstants/PASSWORD password))
    (.createConnection cf)))

(mount/defstate jms-conn
  :start (doto (new-jms-conn (config/mq))
           (.setExceptionListener (proxy [ExceptionListener] []
                                    (onException [^JMSException ex]
                                      (log/fatal ex))))
           (.start))
  :stop (.close jms-conn))