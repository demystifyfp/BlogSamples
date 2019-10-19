(ns wheel.infra.cron.job.core
  (:require [clojurewerkz.quartzite.jobs :as qj]
            [clojurewerkz.quartzite.conversion :as qc]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.schedule.cron :as qsc]
            [clojurewerkz.quartzite.triggers :as qt]
            [wheel.infra.config :as config]))

(defmulti jobtype :type)

(defn- identifier [{:keys [channel-id type]}]
  (keyword channel-id (name type)))

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
   (qt/start-now)
   (qt/with-schedule (qsc/schedule
                      (qsc/cron-schedule expression)))))

(defn create [{:keys [channel-id]
               :as   cron-job-config}]
  (when-let [channel-config (config/get-channel-config channel-id)]
    (create-job channel-config cron-job-config)))

(defn handle [channel-fn ctx]
  (channel-fn (qc/from-job-data ctx) nil))

(defn schedule [scheduler cron-job-config]
  (when-let [job (create cron-job-config)]
    (qs/schedule scheduler job (create-trigger cron-job-config))))

(comment
  (key {:channel-id "UA"
        :type       :allocate-order}))