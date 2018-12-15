module Server

open Saturn
open FSharp.Control.Tasks
open Microsoft.AspNetCore.Http
open FsConfig
open System

let private camelCaseCanonicalizer _ (name : string) =
    name
    |> String.mapi (fun i c ->
      if (i = 0) then Char.ToLowerInvariant c else c
    )

type QueryStringReader(ctx : HttpContext) =
  interface IConfigReader with
    member __.GetValue name =
      printfn "--> %s" name
      match ctx.Request.Query.TryGetValue name with
      | true, x -> Some (x.ToString())
      | _ -> None

let bindQueryString<'T> (ctx : HttpContext) =
  let reader = new QueryStringReader(ctx)
  parse<'T> reader camelCaseCanonicalizer ""

type DealsCategory =
| AllDeals
| AllEBooks
| ActionAndAdventure
| Media
| Fiction

type Language =
| English
| Hindi
| Tamil 

type Rating =
| Five
| FourAndAbove
| ThreeAndAbove

type SearchFilter = {
  Language : Language list
  Rating : Rating option
}

[<CLIMutable>]
type Search = {
  Category : DealsCategory
  Filter : SearchFilter
}

type Book = {
  Title : string
  Author : string
}
let getBooks (ctx : HttpContext) = task {
  let search = bindQueryString<Search> ctx
  printfn "%A" search
  let response = [{Title = "B1"; Author = "A1"}]
  return! Controller.json ctx response
}

type Response = {Message : string}

let bookController = controller {
  index getBooks
}

let apiRouter = router {
  forward "/books" bookController
}

let app = application {
    use_router apiRouter
    url "http://0.0.0.0:8085"
}

[<EntryPoint>]
let main _ =
    printfn "Working directory - %s" (System.IO.Directory.GetCurrentDirectory())
    run app
    0 // return an integer exit code