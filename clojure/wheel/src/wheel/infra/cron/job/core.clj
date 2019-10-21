(ns wheel.infra.cron.job.core
  (:require [clojurewerkz.quartzite.jobs :as qj]
            [clojurewerkz.quartzite.conversion :as qc]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.schedule.cron :as qsc]
            [clojurewerkz.quartzite.triggers :as qt]
            [wheel.infra.config :as config]
            [wheel.middleware.event :as event]
            [wheel.infra.log :as log]))

(defmulti job-type :type)

(defn- identifier [{:keys [channel-id type]}]
  (str channel-id "/" (name type)))

(defn- create-job [channel-config cron-job-config]
  (qj/build
   (qj/of-type (job-type cron-job-config))
   (qj/using-job-data {:channel-config  channel-config
                       :cron-job-config cron-job-config})
   (qj/with-identity (qj/key (identifier cron-job-config)))))

(defn- create-trigger [{:keys [expression]
                        :as   cron-job-config}]
  (qt/build
   (qt/with-identity (qt/key (identifier cron-job-config)))
   (qt/with-schedule (qsc/schedule
                      (qsc/cron-schedule expression)))))

(defn handle [channel-fn ctx]
  (try
    (let [{:strs [channel-config cron-job-config]} (qc/from-job-data ctx)
          {:keys [channel-id type]}                cron-job-config
          {:keys [channel-name]}                   channel-config
          cron-started-event                       (event/cron type channel-id channel-name)]
      (try
        (channel-fn channel-id channel-config)
        (log/write! cron-started-event)
        (catch Throwable ex
          (log/write-all! [cron-started-event
                           (event/cron type channel-id channel-name ex)]))))
    (catch Throwable t
      (prn "~~>" t))))

(defn schedule [scheduler {:keys [channel-id]
                           :as   cron-job-config}]
  (when-let [channel-config (config/get-channel-config channel-id)]
    (let [job     (create-job channel-config cron-job-config)
          trigger (create-trigger cron-job-config)]
      (qs/schedule scheduler job trigger))))

(comment
  (identifier {:channel-id "UA"
               :type       :allocate-order}))