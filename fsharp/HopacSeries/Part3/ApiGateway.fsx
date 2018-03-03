#r "packages/Hopac/lib/net45/Hopac.Core.dll"
#r "packages/Hopac/lib/net45/Hopac.Platform.dll"
#r "packages/Hopac/lib/net45/Hopac.dll"
#r "packages/Hopac/lib/net45/Hopac.dll"
#r "packages/FSharp.Data/lib/net45/FSharp.Data.dll"
#r "packages/Http.fs/lib/net461/HttpFs.dll"
#r "packages/System.Net.Http/lib/net46/System.Net.Http.dll"

open Hopac
open FSharp.Data
open HttpFs.Client

type UserTypeProvider = JsonProvider<"https://api.github.com/users/tamizhvendan">
type User = UserTypeProvider.Root

type ReposTypeProvider = JsonProvider<"https://api.github.com/users/tamizhvendan/repos">
type Repo = ReposTypeProvider.Root

type GitHubResponse = {
  Body : string
  NextPageUrl : string option
}

let getNextPageUrl (linkText : string) = 
  linkText.Split([|','|])
  |> Array.map (fun l -> l.Split([|';'|]))
  |> Array.tryFind(fun l -> l.Length = 2 && l.[1].Contains("next"))
  |> Option.map(fun l -> l.[0].Trim().TrimStart('<').TrimEnd('>'))

let gitHubResponse response = job {
  let! body = Response.readBodyAsString response
  let nextPageUrl =
    match response.headers.TryFind(ResponseHeader.Link) with
    | Some linkText -> getNextPageUrl linkText
    | None -> None
  return {Body = body; NextPageUrl = nextPageUrl}
}

let log msg x =
  printfn "%s" msg
  x

let httpGetWithPagination url =
  Request.createUrl Get url 
  |> Request.setHeader (UserAgent "FsHopac")
  |> log ("Request : " + url)
  |> getResponse
  |> Job.bind gitHubResponse
  |> Job.map (log ("Response : " + url))

let httpGet url =
  httpGetWithPagination url
  |> Job.map (fun r -> r.Body)

let basePath = "https://api.github.com"
let userUrl = sprintf "%s/users/%s" basePath

let getUser username : Job<User> =
  userUrl username
  |> httpGet
  |> Job.map UserTypeProvider.Parse

let userReposUrl = sprintf "%s/users/%s/repos?per_page=100" basePath

let topThreeUserRepos (repos : Repo []) =
  let takeCount =
    let reposCount = Array.length repos
    if reposCount > 3 then 3 else reposCount
  repos
  |> Array.filter (fun repo -> not repo.Fork)
  |> Array.sortByDescending (fun repo -> repo.StargazersCount)
  |> Array.take takeCount

let getUserAllRepos username =
  let rec getUserAllRepos' url (repos : Repo list) = job {
    let! gitHubResponse = 
      httpGetWithPagination url
    let currentPageRepos = 
      gitHubResponse.Body
      |> ReposTypeProvider.Parse
      |> Array.toList
    let reposSoFar = repos @ currentPageRepos
    match gitHubResponse.NextPageUrl with
    | Some nextPageUrl ->
       return! getUserAllRepos' nextPageUrl reposSoFar
    | None -> return reposSoFar
  }
  getUserAllRepos' (userReposUrl username) []
  |> Job.map (List.toArray)

let getTopThreeUserRepos username : Job<Repo []> =
  getUserAllRepos username
  |> Job.map topThreeUserRepos


let languagesUrl userName repoName  = 
  sprintf "%s/repos/%s/%s/languages" basePath userName repoName

let parseLanguagesJson languagesJson =
  languagesJson
  |> JsonValue.Parse
  |> JsonExtensions.Properties
  |> Array.map fst

let getUserRepoLanguages username repoName =
  languagesUrl username repoName 
  |> httpGet
  |> Job.map parseLanguagesJson

type RepoDto = {
  Name : string
  StargazersCount : int
  Languages : string []
}

type UserDto = {
  Name : string
  AvatarUrl : string
  TopThreeRepos : RepoDto []
}

let repoDto (repo : Repo) languages = {
  Name = repo.Name
  StargazersCount = repo.StargazersCount
  Languages = languages
}

open Hopac.Infixes

let getRepoDto username (repo : Repo) =
  getUserRepoLanguages username repo.Name
  |> Job.map (repoDto repo)

let getUserDto username = job {
  let! user, repos = 
    getUser username <*> getTopThreeUserRepos username
  let! repoDtos = 
    repos 
    |> Array.map (getRepoDto username)
    |> Job.conCollect
  return {
    Name = user.Name 
    AvatarUrl = user.AvatarUrl
    TopThreeRepos = repoDtos.ToArray()
  }
}

#time "on"
getUserDto "haf" |> run
#time "off"