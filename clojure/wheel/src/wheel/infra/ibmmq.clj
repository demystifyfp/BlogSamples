(ns wheel.infra.ibmmq
  (:import (com.ibm.msg.client.jms JmsFactoryFactory)
           (com.ibm.msg.client.wmq WMQConstants))
  (:require [wheel.infra.config :as config]
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
  :start (let [conn (new-jms-conn (config/mq))]
           (.start conn)
           conn)
  :stop (.close jms-conn))