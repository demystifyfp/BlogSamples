#r "packages/Hopac/lib/net45/Hopac.Core.dll"
#r "packages/Hopac/lib/net45/Hopac.Platform.dll"
#r "packages/Hopac/lib/net45/Hopac.dll"

open Hopac

type JobStatus =
| Started of int
| Completed of int

let rec jobStatusPrinterJob jobStatusChannel = job {
  let! jobStatus = Ch.take jobStatusChannel
  match jobStatus with
  | Started jobId -> 
    printfn "starting job:%d" jobId
  | Completed jobId ->
    printfn "completed job:%d" jobId
}

let createJob jobStatusChannel jobId = job {
  do! Ch.give jobStatusChannel (Started jobId)
  do! timeOutMillis (jobId * 1000)
  do! Ch.give jobStatusChannel (Completed jobId)
}

let main jobStatusChannel jobsCount = job {
  let jobStatusPrinter = jobStatusPrinterJob jobStatusChannel
  do! Job.foreverServer jobStatusPrinter 
  let myJobs = List.init jobsCount (createJob jobStatusChannel)
  return! Job.conIgnore myJobs
}

let jobStatusChannel = Ch<JobStatus>()
let jobsCount =  5 

main jobStatusChannel jobsCount |> run