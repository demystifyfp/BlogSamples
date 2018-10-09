#r "./packages/Hopac/lib/net45/Hopac.Core.dll"
#r "./packages/Hopac/lib/net45/Hopac.Platform.dll"
#r "./packages/Hopac/lib/net45/Hopac.dll"
#r "./packages/Http.fs/lib/net471/HttpFs.dll"

open Hopac
open Hopac.Stream
open HttpFs.Client

let urls = 
  [ "http://bing.com" 
    "http://yahoo.com"
    "http://google.com"
    "http://msn.com"]

let pages = appended {
  for url in urls do
    printfn "fetching %s" url
    let! body = 
      Request.createUrl Get url
      |> Request.responseAsString
      |> Stream.once
    printfn "retrived %s" url
    yield (url, body.Length)
}

let run () =
  pages
  |> Stream.filterFun (fun (_, len) -> len < 50000)
  |> Stream.mapFun fst
  |> Stream.iterFun (fun url -> printfn "%s" url)
  |> run

  