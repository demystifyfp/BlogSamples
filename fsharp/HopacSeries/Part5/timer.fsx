#r "packages/Hopac/lib/net45/Hopac.Core.dll"
#r "packages/Hopac/lib/net45/Hopac.Platform.dll"
#r "packages/Hopac/lib/net45/Hopac.dll"

open Hopac
open Hopac.Infixes
open System

type Ticker (timeSpan : TimeSpan) =
  let tickCh = Ch<DateTimeOffset>()
  let cancelled = IVar()

  let tick () =
    Ch.give tickCh DateTimeOffset.Now

  let rec loop () = 
    let tickerLoop =
      timeOut timeSpan
      |> Alt.afterJob tick
      |> Alt.afterJob loop
    tickerLoop <|> IVar.read cancelled

  do start (loop())
  
  member __.Stop() = 
    IVar.tryFill cancelled () |> start
  member __.C
    with get() = tickCh

let useCase1 nTimes (xSeconds : int) f =
  let timeSpan = 
    xSeconds |> float |> TimeSpan.FromSeconds 
  let ticker = new Ticker(timeSpan)
  let onTick = f
  ticker.C 
  |> Alt.afterFun onTick
  |> Job.forN nTimes
  |> run
  ticker.Stop()

// useCase1 5 2 (printfn "%A")

let ticker seconds =
  seconds
  |> float 
  |> TimeSpan.FromSeconds
  |> Ticker

let useCase2 t1Interval t2Interval xMillis =
  let ticker1 = ticker t1Interval
  let ticker2 = ticker t2Interval

  let onTick tCh name loop =
    tCh 
      |> Alt.afterFun (fun t -> printfn "[%s] at %A" name t)
      |> Alt.afterJob loop
  
  let rec loop () =
    onTick ticker1.C "T1" loop <|> onTick ticker2.C "T2" loop
  
  printfn "Starts at %A" (DateTimeOffset.Now)
  start (loop())

  let onTimeOut _ = 
    ticker1.Stop()
    ticker2.Stop()

  timeOutMillis xMillis
  |> Alt.afterFun onTimeOut
  |> run
  printfn "Ends at %A" (DateTimeOffset.Now)
  