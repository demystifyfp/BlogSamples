(ns wheel.infra.cron.core
  (:require [clojurewerkz.quartzite.scheduler :as qs]
            [wheel.infra.cron.job.allocate-order]
            [wheel.infra.cron.job.core :as job]
            [wheel.infra.config :as config]
            [mount.core :as mount]))

(mount/defstate scheduler
  :start (qs/start (qs/initialize))
  :stop (qs/shutdown scheduler))

(defn init []
  (for [cron-job-config (config/get-all-cron-jobs)]
    (job/schedule scheduler cron-job-config)))
