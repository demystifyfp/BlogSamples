(ns wheel.infra.cron.job.allocate-order
  (:require [wheel.infra.cron.job.core :as job]
            [clojurewerkz.quartzite.jobs :as qj]
            [wheel.marketplace.channel :as channel]))

(qj/defjob AllocateOrderJob [ctx]
  (job/handle channel/allocate-order ctx))

(defmethod job/job-type :allocate-order [_]
  AllocateOrderJob)