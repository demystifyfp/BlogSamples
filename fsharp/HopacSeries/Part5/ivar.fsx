#r "packages/Hopac/lib/net45/Hopac.Core.dll"
#r "packages/Hopac/lib/net45/Hopac.Platform.dll"
#r "packages/Hopac/lib/net45/Hopac.dll"

open Hopac

let fillAndRead iVar value =
  IVar.fill iVar value
  |> Job.bind (fun _ -> IVar.read iVar)
  |> Job.map (printfn "%A")
  |> run

fillAndRead (IVar<bool>()) true
fillAndRead (IVar<bool>()) false

let intIVar = IVar<int>()
fillAndRead intIVar 42
fillAndRead intIVar 42 

let tryFillAndRead iVar value =
  IVar.tryFill iVar value
  |> Job.bind (fun _ -> IVar.read iVar)
  |> Job.map (printfn "%A")
  |> run

let anotherIntIVar = IVar<int>()
tryFillAndRead anotherIntIVar 42
tryFillAndRead anotherIntIVar 10

open Hopac.Infixes

let readOrTimeout delayInMillis iVar =
  let timeOutAlt =
    timeOutMillis delayInMillis 
    |> Alt.afterFun (fun _ -> printfn "time out!")
  let readAlt =
    IVar.read iVar
    |> Alt.afterFun (printfn "%A")
  timeOutAlt <|> readAlt
  |> run

let yetAnotherIntIVar = IVar<int>()
tryFillAndRead yetAnotherIntIVar 10
readOrTimeout 1000 intIVar

#time "on"
readOrTimeout 2000 (IVar<unit>())
#time "off"