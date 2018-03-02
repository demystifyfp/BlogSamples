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


let httpGet url =
  Request.createUrl Get url 
  |> Request.setHeader (UserAgent "FsHopac")
  |> getResponse
  |> Job.bind Response.readBodyAsString

let host = "https://api.github.com"
let userUrl = sprintf "%s/users/%s" host

let getUser username : Job<User> =
  userUrl username
  |> httpGet
  |> Job.map UserTypeProvider.Parse

let userReposUrl = sprintf "%s/users/%s/repos" host

let topThreeUserRepos (repos : Repo []) =
  let takeCount =
    let reposCount = Array.length repos
    if reposCount > 3 then 3 else reposCount
  repos
  |> Array.filter (fun repo -> not repo.Fork)
  |> Array.sortByDescending (fun repo -> repo.StargazersCount)
  |> Array.take takeCount

let getTopThreeUserRepos username : Job<Repo []> =
  userReposUrl username
  |> httpGet
  |> Job.map ReposTypeProvider.Parse
  |> Job.map topThreeUserRepos

let languagesUrl userName repoName  = 
  sprintf "%s/repos/%s/%s/languages" host userName repoName

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