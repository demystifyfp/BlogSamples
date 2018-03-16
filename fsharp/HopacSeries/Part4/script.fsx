#r "packages/Hopac/lib/net45/Hopac.Core.dll"
#r "packages/Hopac/lib/net45/Hopac.Platform.dll"
#r "packages/Hopac/lib/net45/Hopac.dll"

open Hopac
open Hopac.Infixes

// let delayedPrintn msg delayInMillis =
//   timeOutMillis delayInMillis
//   |> Job.map (fun _ -> printfn "%s" msg)

// let delayedPrintn msg delayInMillis =
//   timeOutMillis delayInMillis
//   |> Alt.afterFun (fun _ -> printfn "%s" msg)

let delayedPrintn msg delayInMillis =
  Alt.prepareFun <| fun _ -> 
    printfn "starting [%s]" msg
    timeOutMillis delayInMillis
    |> Alt.afterFun (fun _ -> printfn "%s" msg)

#time "on"
delayedPrintn "Hi" 3000 |> run
#time "off"

let delayedHiPrinter = delayedPrintn "Hi" 2000
let delayedHelloPrinter = delayedPrintn "Hello" 1000

let runThemParallel () = 
  delayedHiPrinter <*> delayedHelloPrinter 
  |> run |> ignore

#time "on"
runThemParallel ()
#time "off"
let chooseBetweenThem () =
  delayedHiPrinter <|> delayedHelloPrinter 
  |> run

#time "on"
chooseBetweenThem ()
#time "off"

let delayedPrintnWithNack msg delayInMillis =
  let onNack nack =
    nack
    |> Alt.afterFun (fun _ -> printfn "aborting [%s]" msg)
  Alt.withNackJob <| fun nack -> 
    Job.start (onNack nack)
    |> Job.map (fun _ -> delayedPrintn msg delayInMillis)

let delayedHiPrinterWithNack = 
  delayedPrintnWithNack "Hi" 2000
let delayedHelloPrinterWithNack = 
  delayedPrintnWithNack "Hello" 1000

let chooseBetweenThemWithNack () =
  delayedHiPrinterWithNack <|> delayedHelloPrinterWithNack 
  |> run

#time "on"
chooseBetweenThemWithNack ()
#time "off"