#load "ticker.fsx"
open System
open Ticker
open Hopac

type Reason =
| Exploded
| Defused

type Status =
| NotActivated
| Alive
| Dead of Reason


type TimeBomb () = 

  let reason = IVar<Reason>()
  let activated = IVar()
  let secondsRemainingCh = Ch<int>()
  let inCh = Ch<char>()
  let rec onTick (ticker : Ticker) secondsRemaining =
    ticker.C
    |> Alt.afterJob (fun _ -> Ch.send secondsRemainingCh secondsRemaining)
    |> Alt.afterJob (fun _ -> onTick ticker (secondsRemaining - 1))

  let startTicker seconds =
    let ticker = new Ticker(TimeSpan.FromSeconds 1.)
    onTick ticker (seconds - 1) |> start
    ticker

  let startTimeOut seconds =
    let timeOutAlt = 
      seconds 
      |> float 
      |> TimeSpan.FromSeconds 
      |> timeOut
    
    timeOutAlt
    |> Alt.afterJob (fun _ -> 
        IVar.tryFill reason Exploded)
    |> start

  let rec inputLoop defuseChar =
    let onInput inChar =
      if inChar = defuseChar then
        IVar.tryFill reason Defused
      else
        inputLoop defuseChar :> Job<unit>
    inCh
    |> Alt.afterJob onInput
  let activate seconds defuseChar =
    let ticker = startTicker seconds
    startTimeOut seconds
    IVar.tryFill activated () |> start
    inputLoop defuseChar |> start
    IVar.read reason
    |> Alt.afterFun (fun _ -> ticker.Stop())
    |> start

  member __.Status 
    with get() =
      let deadReasonAlt =
        IVar.read reason
        |> Alt.afterFun Dead

      let activatedAlt =
        IVar.read activated
        |> Alt.afterFun (fun _ -> Alive)
      
      let notActivatedAlt =
        Alt.always NotActivated

      Alt.choose [
        deadReasonAlt 
        activatedAlt
        notActivatedAlt] 
      |> run

  member this.Activate (seconds : int, defuseChar : char) =
    match this.Status with
    | NotActivated -> 
      activate seconds defuseChar
    | _ -> ()

  member this.TryDefuse(defuseChar) =
    match this.Status with
    | Alive -> 
      Ch.give inCh defuseChar 
      |> start
    | _ -> ()

  member __.SecondsRemainingCh
    with get() = secondsRemainingCh

  member __.DeadStatusAlt
    with get() = IVar.read reason


let printSecondsRemaining (t : TimeBomb) =
  t.SecondsRemainingCh
  |> Alt.afterFun (printfn "Seconds Remaining: %d")
  |> Job.foreverServer |> start

let simulateExplosion () =
  let seconds = 5
  let t = TimeBomb()
  t.Status |> printfn "Status: %A"
  t.Activate(seconds, 'a')
  printSecondsRemaining t
  t.Status |> printfn "Status: %A"
  t.DeadStatusAlt
  |> Alt.afterFun (printfn "Status: %A")
  |> run

let simulateDefuse char =
  let seconds = 5
  let t = TimeBomb()
  t.Status |> printfn "Status: %A"
  t.Activate(seconds, 'a')
  printSecondsRemaining t
  t.Status |> printfn "Status: %A"
  
  TimeSpan.FromSeconds 3.
  |> timeOut 
  |> Alt.afterFun (fun _ -> t.TryDefuse(char))
  |> Alt.afterJob (fun _ -> t.DeadStatusAlt)
  |> Alt.afterFun (printfn "Status: %A")
  |> run