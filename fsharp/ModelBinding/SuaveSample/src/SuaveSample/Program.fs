module SuaveSample

open System
open Suave
open Suave.Operators
open Suave.Filters
open FsConfig

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

type Search = {
  Category : DealsCategory
  Filter : SearchFilter
}

let queryStringsMap (request : HttpRequest) =
  request.query
  |> List.groupBy fst
  |> List.map (fun (x, keyVals) -> 
                (x, keyVals |> List.map snd |> List.choose id |> String.concat ","))
  |> Map.ofList

type HttpQueryStringsProvider(request : HttpRequest) =
    let queryStringsMap = queryStringsMap request

    interface IConfigReader with
      member __.GetValue name =
        Map.tryFind name queryStringsMap

let private camelCaseCanonicalizer _ (name : string) =
    name
    |> String.mapi (fun i c ->
      if (i = 0) then Char.ToLowerInvariant c else c)

let bindQueryStrings<'T> (request : HttpRequest) =
  let queryStringsProvider = new HttpQueryStringsProvider(request)
  parse<'T> queryStringsProvider camelCaseCanonicalizer ""
  |> Result.mapError (fun e -> e.ToString())

let getBooks ctx = async {
  let search = bindQueryStrings<Search> ctx.request
  printfn "%A" search
  return! Successful.OK "Todo" ctx
}

let app =
  path "/books" >=> getBooks

[<EntryPoint>]
let main argv = 
  startWebServer defaultConfig app
  0 // return an integer exit code