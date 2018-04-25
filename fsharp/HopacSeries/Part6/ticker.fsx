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