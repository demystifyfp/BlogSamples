(ns wheel.infra.cron.job.core
  (:require [clojurewerkz.quartzite.jobs :as qj]
            [clojurewerkz.quartzite.conversion :as qc]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.schedule.cron :as qsc]
            [clojurewerkz.quartzite.triggers :as qt]
            [wheel.infra.config :as config]))

(defmulti jobtype :type)

(defn- identifier [{:keys [channel-id type]}]
  (str channel-id "/" (name type)))

(defn- create-job [channel-config cron-job-config]
  (qj/build
   (qj/of-type (jobtype cron-job-config))
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
  (let [{:strs [channel-config cron-job-config]} (qc/from-job-data ctx)]
    (try
      (channel-fn (:channel-id cron-job-config) channel-config)
      (catch Throwable ex
        (prn "==>" ex)))))

(defn schedule [scheduler {:keys [channel-id]
                           :as   cron-job-config}]
  (when-let [channel-config (config/get-channel-config channel-id)]
    (let [job     (create-job channel-config cron-job-config)
          trigger (create-trigger cron-job-config)]
      (qs/schedule scheduler job trigger))))

(comment
  (identifier {:channel-id "UA"
               :type       :allocate-order}))