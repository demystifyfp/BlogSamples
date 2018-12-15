module Server

open Saturn
open FSharp.Control.Tasks
open Microsoft.AspNetCore.Http


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
  Filter : SearchFilter option
}

type Book = {
  Title : string
  Author : string
}
let getBooks (ctx : HttpContext) = task {
  let qs = ctx.Request.Query
  qs |> Seq.iter (printfn "%A")
  let search = Controller.getQuery<Search> ctx
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